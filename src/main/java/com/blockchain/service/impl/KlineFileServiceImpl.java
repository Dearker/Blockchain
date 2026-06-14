package com.blockchain.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.blockchain.config.KlineProperties;
import com.blockchain.constants.BinanceApiConstants;
import com.blockchain.domain.KlineData;
import com.blockchain.service.KlineFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * K线数据文件存储服务实现类
 * 所有时间级别写入同一个 CSV 文件，通过 timeInterval 列区分
 *
 * @author blockchain
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KlineFileServiceImpl implements KlineFileService {

    private final KlineProperties klineProperties;

    // ==================== 公开接口 ====================

    @Override
    public void syncKlineData(String symbol) {
        log.info("开始同步K线数据到本地文件, 交易对: {}", symbol);

        // 创建交易对目录
        FileUtil.mkdir(new File(klineProperties.getPath(), symbol));

        // 针对每个时间级别进行同步（每个级别写入同一个 kline.csv）
        for (String interval : BinanceApiConstants.SUPPORTED_INTERVALS) {
            int targetCount = BinanceApiConstants.INTERVAL_TARGET_COUNT.get(interval);
            syncIntervalToFile(symbol, interval, targetCount);
        }

        log.info("K线数据文件同步完成, 交易对: {}", symbol);
    }

    @Override
    public void syncKlineData(List<String> symbols) {
        symbols.forEach(this::syncKlineData);
    }

    @Override
    public String readLocalKlineData(String symbol, String interval) {
        String filePath = getKlineFilePath(symbol);
        if (!FileUtil.exist(filePath)) return null;

        // 若指定了 interval，只返回该级别的数据行（含表头）
        if (interval != null) {
            List<String> lines = FileUtil.readLines(filePath, StandardCharsets.UTF_8);
            StringBuilder sb = new StringBuilder();
            sb.append(CSV_HEADER).append("\n");
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line != null && line.startsWith(interval + ",")) {
                    sb.append(line).append("\n");
                }
            }
            return sb.toString();
        }
        return FileUtil.readString(filePath, StandardCharsets.UTF_8);
    }

    // ==================== 核心同步逻辑 ====================

    /**
     * 同步单个时间级别
     * 逻辑：读全量CSV → 移除该 interval 旧数据 → 合并新数据 → 按 interval 分别截断 → 全量写回
     */
    private void syncIntervalToFile(String symbol, String interval, int targetCount) {
        // 从同步记录读取上次进度
        Map<String, Long> syncRecord = readSyncRecord(symbol);
        Long lastOpenTime = syncRecord.get(interval + "_lastOpenTime");

        // 读取全量现有数据（所有时间级别）
        List<KlineData> allExisting = readAllExistingKlineData(symbol);

        // 拉取新数据
        List<KlineData> newData;
        if (lastOpenTime == null) {
            newData = fetchFullKlineData(symbol, interval, targetCount);
        } else {
            newData = fetchIncrementalKlineData(symbol, interval, lastOpenTime);
        }

        if (newData.isEmpty()) {
            log.info("没有新的K线数据: {} {}", symbol, interval);
            return;
        }

        // 移除该 interval 的旧数据，合并新数据
        List<KlineData> filtered = allExisting.stream()
                .filter(d -> !interval.equals(d.getTimeInterval()))
                .collect(Collectors.toList());
        filtered.addAll(newData);

        // 按 interval 分组，每个 interval 保留 targetCount 条（按 openTime 降序）
        List<KlineData> mergedData = limitPerInterval(filtered);

        // 全量写回同一个文件
        try {
            writeAllKlineData(symbol, mergedData);
        } catch (IOException e) {
            log.error("写入CSV文件失败: {} {}", symbol, interval, e);
            throw new RuntimeException("写入K线数据文件失败", e);
        }

        // 更新同步记录（取该 interval 最新的 openTime）
        Long latestOpenTime = mergedData.stream()
                .filter(d -> interval.equals(d.getTimeInterval()))
                .map(KlineData::getOpenTime)
                .max(Long::compareTo)
                .orElse(newData.get(0).getOpenTime());
        updateSyncRecord(symbol, interval, latestOpenTime);

        log.info("同步完成: {} {} 新增{}条，共{}条", symbol, interval, newData.size(),
                mergedData.stream().filter(d -> interval.equals(d.getTimeInterval())).count());
    }

    /**
     * 按 interval 分组，每个 interval 保留 targetCount 条（降序取最新）
     */
    private List<KlineData> limitPerInterval(List<KlineData> data) {
        Map<String, Integer> targetMap = BinanceApiConstants.INTERVAL_TARGET_COUNT;
        return data.stream()
                .collect(Collectors.groupingBy(KlineData::getTimeInterval))
                .entrySet().stream()
                .flatMap(entry -> {
                    String interval = entry.getKey();
                    List<KlineData> list = entry.getValue();
                    int limit = targetMap.getOrDefault(interval, list.size());
                    return list.stream()
                            .sorted(Comparator.comparing(KlineData::getOpenTime).reversed())
                            .limit(limit);
                })
                .collect(Collectors.toList());
    }

    // ==================== CSV 读写 ====================

    /**
     * 读取全量K线数据（所有时间级别）
     * interval 为 null 时返回全量；指定时只返回该级别
     */
    private List<KlineData> readExistingKlineData(String symbol, String interval) {
        String filePath = getKlineFilePath(symbol);
        if (!FileUtil.exist(filePath)) {
            return new ArrayList<>();
        }

        List<KlineData> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        List<String> lines = FileUtil.readLines(filePath, StandardCharsets.UTF_8);

        // 跳过表头（第0行）
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.trim().isEmpty()) continue;
            String[] fields = line.split(",", -1);
            if (fields.length < 12) continue; // timeInterval + 11个数据字段

            // 若指定了 interval，只保留匹配的行的数据
            if (interval != null && !interval.equals(fields[0])) continue;

            try {
                KlineData data = KlineData.builder()
                        .symbol(symbol)
                        .timeInterval(fields[0])
                        .openTime(parseLongField(fields[1]))
                        .openPrice(new BigDecimal(fields[2]))
                        .highPrice(new BigDecimal(fields[3]))
                        .lowPrice(new BigDecimal(fields[4]))
                        .closePrice(new BigDecimal(fields[5]))
                        .volume(new BigDecimal(fields[6]))
                        .closeTime(parseLongField(fields[7]))
                        .quoteAssetVolume(new BigDecimal(fields[8]))
                        .numberOfTrades(Integer.parseInt(fields[9]))
                        .takerBuyBaseAssetVolume(new BigDecimal(fields[10]))
                        .takerBuyQuoteAssetVolume(new BigDecimal(fields[11]))
                        .createTime(now)
                        .updateTime(now)
                        .build();
                result.add(data);
            } catch (Exception e) {
                log.warn("跳过无效CSV行: {} - {}", line, e.getMessage());
            }
        }
        return result;
    }

    /**
     * 读取全量数据（所有时间级别，不筛选）
     */
    private List<KlineData> readAllExistingKlineData(String symbol) {
        return readExistingKlineData(symbol, null);
    }

    /**
     * 将全量数据写入同一个 CSV 文件（原子写入）
     * 按 4h → 1d → 1w 顺序分组写入，每组内按 openTime 降序
     */
    private void writeAllKlineData(String symbol, List<KlineData> allData) throws IOException {
        String filePath = getKlineFilePath(symbol);
        File targetFile = new File(filePath);
        FileUtil.mkParentDirs(filePath);

        // 按时间级别分组，顺序：4h → 1d → 1w
        List<String> intervalOrder = Arrays.asList("4h", "1d", "1w");
        Map<String, List<KlineData>> grouped = allData.stream()
                .collect(Collectors.groupingBy(KlineData::getTimeInterval));

        Path tmpPath = targetFile.toPath().resolveSibling("kline.tmp");
        StringBuilder sb = new StringBuilder();
        sb.append(CSV_HEADER).append("\n");

        for (String interval : intervalOrder) {
            List<KlineData> group = grouped.get(interval);
            if (group == null) continue;
            // 每组内按 openTime 降序
            group.sort((a, b) -> Long.compare(b.getOpenTime(), a.getOpenTime()));
            for (KlineData d : group) {
                sb.append(d.getTimeInterval() != null ? d.getTimeInterval() : "").append(",")
                  .append(d.getOpenTime() != null ? d.getOpenTime() : "").append(",")
                  .append(d.getOpenPrice() != null ? d.getOpenPrice() : "").append(",")
                  .append(d.getHighPrice() != null ? d.getHighPrice() : "").append(",")
                  .append(d.getLowPrice() != null ? d.getLowPrice() : "").append(",")
                  .append(d.getClosePrice() != null ? d.getClosePrice() : "").append(",")
                  .append(d.getVolume() != null ? d.getVolume() : "").append(",")
                  .append(d.getCloseTime() != null ? d.getCloseTime() : "").append(",")
                  .append(d.getQuoteAssetVolume() != null ? d.getQuoteAssetVolume() : "").append(",")
                  .append(d.getNumberOfTrades() != null ? d.getNumberOfTrades() : "").append(",")
                  .append(d.getTakerBuyBaseAssetVolume() != null ? d.getTakerBuyBaseAssetVolume() : "").append(",")
                  .append(d.getTakerBuyQuoteAssetVolume() != null ? d.getTakerBuyQuoteAssetVolume() : "")
                  .append("\n");
            }
        }

        Files.writeString(tmpPath, sb.toString(), StandardCharsets.UTF_8);
        Files.move(tmpPath, targetFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    // ==================== API 拉取 ====================

    private List<KlineData> fetchFullKlineData(String symbol, String interval, int targetCount) {
        List<KlineData> allData = new ArrayList<>();
        Long endTime = null;

        while (allData.size() < targetCount) {
            int limit = Math.min(BinanceApiConstants.API_MAX_LIMIT, targetCount - allData.size());
            List<KlineData> batch = fetchFromApi(symbol, interval, null, endTime, limit);
            if (batch.isEmpty()) break;
            allData.addAll(batch);
            endTime = batch.get(batch.size() - 1).getOpenTime() - 1;
        }
        return allData;
    }

    private List<KlineData> fetchIncrementalKlineData(String symbol, String interval, Long startTime) {
        return fetchFromApi(symbol, interval, startTime, null, BinanceApiConstants.API_MAX_LIMIT);
    }

    private List<KlineData> fetchFromApi(String symbol, String interval, Long startTime, Long endTime, int limit) {
        String url = buildApiUrl(symbol, interval, startTime, endTime, limit);
        log.debug("请求API: {}", url);

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String response = HttpUtil.get(url, 30000);
                JSONArray jsonArray = JSON.parseArray(response);
                if (jsonArray == null) {
                    log.warn("API返回为空（第{}次）: {}", attempt, url);
                    if (attempt < maxRetries) continue;
                    return new ArrayList<>();
                }

                List<KlineData> result = new ArrayList<>();
                LocalDateTime now = LocalDateTime.now();

                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONArray arr = jsonArray.getJSONArray(i);
                    result.add(KlineData.builder()
                            .symbol(symbol)
                            .timeInterval(interval)
                            .openTime(arr.getLong(0))
                            .openPrice(arr.getBigDecimal(1))
                            .highPrice(arr.getBigDecimal(2))
                            .lowPrice(arr.getBigDecimal(3))
                            .closePrice(arr.getBigDecimal(4))
                            .volume(arr.getBigDecimal(5))
                            .closeTime(arr.getLong(6))
                            .quoteAssetVolume(arr.getBigDecimal(7))
                            .numberOfTrades(arr.getInteger(8))
                            .takerBuyBaseAssetVolume(arr.getBigDecimal(9))
                            .takerBuyQuoteAssetVolume(arr.getBigDecimal(10))
                            .createTime(now)
                            .updateTime(now)
                            .build());
                }
                return result;
            } catch (Exception e) {
                log.warn("拉取失败（第{}次/共{}次）: {}", attempt, maxRetries, url, e);
                if (attempt >= maxRetries) {
                    log.error("拉取失败，已重试{}次: {}", maxRetries, url, e);
                    throw new RuntimeException("拉取币安K线数据失败", e);
                }
                try { Thread.sleep(500L * attempt); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
        return new ArrayList<>();
    }

    private String buildApiUrl(String symbol, String interval, Long startTime, Long endTime, int limit) {
        StringBuilder sb = new StringBuilder(BinanceApiConstants.KLINES_API_URL);
        sb.append("?symbol=").append(symbol);
        sb.append("&interval=").append(interval);
        sb.append("&limit=").append(limit);
        if (startTime != null) sb.append("&startTime=").append(startTime);
        if (endTime != null) sb.append("&endTime=").append(endTime);
        return sb.toString();
    }

    // ==================== 同步记录 ====================

    private String getKlineFilePath(String symbol) {
        return new File(new File(klineProperties.getPath(), symbol), "kline.csv").getAbsolutePath();
    }

    private String getSyncRecordFilePath(String symbol) {
        return new File(new File(klineProperties.getPath(), symbol), BinanceApiConstants.SYNC_RECORD_FILE_NAME).getAbsolutePath();
    }

    private Map<String, Long> readSyncRecord(String symbol) {
        String filePath = getSyncRecordFilePath(symbol);
        if (!FileUtil.exist(filePath)) {
            return new HashMap<>();
        }
        String content = FileUtil.readString(filePath, StandardCharsets.UTF_8);
        JSONObject json = JSON.parseObject(content);
        Map<String, Long> result = new HashMap<>();
        for (String key : json.keySet()) {
            result.put(key, json.getLong(key));
        }
        return result;
    }

    private void updateSyncRecord(String symbol, String interval, Long lastOpenTime) {
        Map<String, Long> record = readSyncRecord(symbol);
        record.put(interval + "_lastOpenTime", lastOpenTime);
        record.put(interval + "_lastSyncTime", System.currentTimeMillis());

        String filePath = getSyncRecordFilePath(symbol);
        File targetFile = new File(filePath);
        FileUtil.mkParentDirs(filePath);

        try {
            Path tmpPath = targetFile.toPath().resolveSibling(BinanceApiConstants.SYNC_RECORD_FILE_NAME + ".tmp");
            Files.writeString(tmpPath, JSON.toJSONString(record), StandardCharsets.UTF_8);
            Files.move(tmpPath, targetFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("写入同步记录失败: {}", filePath, e);
            throw new RuntimeException("写入同步记录失败", e);
        }
    }

    // ==================== 工具方法 ====================

    private static final String CSV_HEADER =
            "timeInterval,openTime,openPrice,highPrice,lowPrice,closePrice,volume,closeTime," +
            "quoteAssetVolume,numberOfTrades,takerBuyBaseAssetVolume,takerBuyQuoteAssetVolume";

    private Long parseLongField(String value) {
        return (value == null || value.trim().isEmpty()) ? 0L : Long.parseLong(value.trim());
    }
}
