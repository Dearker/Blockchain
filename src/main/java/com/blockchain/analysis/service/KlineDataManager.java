package com.blockchain.analysis.service;

import com.alibaba.fastjson2.JSONArray;
import com.blockchain.analysis.model.KlineData;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

/**
 * K线数据管理：CSV读写、币安API拉取、数据合并与裁剪
 *
 * 对应Python版 KlineDataManager 类
 */
@Slf4j
//@Service
public class KlineDataManager {

    /** CSV文件列名（与币安合约K线API返回字段顺序一致） */
    private static final String[] CSV_HEADER = {
            "timestamp", "open", "high", "low", "close", "volume",
            "close_time", "quote_volume", "trades",
            "taker_buy_base", "taker_buy_quote", "ignore"
    };

    /** 币安合约K线API地址 */
    private static final String BINANCE_URL = "https://fapi.binance.com/fapi/v1/klines";

    private final String dataDir;
    private final HttpClient httpClient;

    public KlineDataManager() {
        // 默认数据目录（与Python版保持一致）
        this("D:/kdata/lines");
    }

    public KlineDataManager(String dataDir) {
        this.dataDir = dataDir;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        // 确保数据目录存在
        try {
            Files.createDirectories(Paths.get(dataDir));
        } catch (IOException e) {
            log.error("创建数据目录失败: {}", dataDir, e);
        }
    }

    /**
     * 获取CSV文件路径
     * 路径格式: {dataDir}/{symbol}/{symbol}_{tf}.csv
     */
    private String csvPath(String symbol, String tf) {
        return dataDir + "/" + symbol + "/" + symbol + "_" + tf + ".csv";
    }

    /**
     * 确保交易对目录存在
     */
    private void ensureSymbolDir(String symbol) throws IOException {
        Path dir = Paths.get(dataDir, symbol);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
            log.info("已创建交易对目录: {}", dir);
        }
    }

    /**
     * 从本地CSV加载K线数据
     * @return List<KlineData>，如果文件不存在返回空List
     */
    public List<KlineData> loadLocalData(String symbol, String tf) {
        String path = csvPath(symbol, tf);
        File file = new File(path);
        if (!file.exists()) {
            log.info("本地CSV不存在: {}", path);
            return new ArrayList<>();
        }
        try (CSVReader reader = new CSVReader(new FileReader(file))) {
            List<String[]> rows = reader.readAll();
            if (rows.isEmpty()) return new ArrayList<>();

            // 跳过表头
            int startIdx = rows.get(0)[0].equals(CSV_HEADER[0]) ? 1 : 0;
            List<KlineData> result = new ArrayList<>();
            for (int i = startIdx; i < rows.size(); i++) {
                result.add(KlineData.fromCsvRow(rows.get(i)));
            }
            log.info("  ✓ 本地加载: {}根K线", result.size());
            return result;
        } catch (IOException | CsvException e) {
            log.error("读取CSV失败: {}", path, e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取最新的时间戳（毫秒）
     */
    public long getLatestTimestamp(List<KlineData> data) {
        if (data == null || data.isEmpty()) return 0L;
        return data.get(data.size() - 1).getTimestamp();
    }

    /**
     * 从币安API拉取K线数据
     * @param symbol 交易对（如 BTCUSDT）
     * @param interval 时间间隔（如 4h, 1d, 1w）
     * @param limit 拉取数量
     * @param startTime 起始时间（ms，可选）
     */
    public List<KlineData> fetchKlineData(String symbol, String interval, int limit, Long startTime) {
        String url = BINANCE_URL + "?symbol=" + symbol
                + "&interval=" + interval + "&limit=" + limit
                + (startTime != null ? "&startTime=" + startTime : "");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("  ✗ API返回错误码: {}", response.statusCode());
                return new ArrayList<>();
            }
            List<KlineData> result = parseBinanceResponse(response.body());
            log.info("  ✓ 拉取成功: {}根", result.size());
            return result;
        } catch (Exception e) {
            log.error("  ✗ 拉取失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 解析币安API返回的JSON数据
     * 币安返回: [[timestamp, open, high, low, close, volume, closeTime, ...], ...]
     * 使用 fastjson2 解析，避免手动字符串处理的边界问题
     */
    private List<KlineData> parseBinanceResponse(String json) {
        List<KlineData> result = new ArrayList<>();
        try {
            JSONArray root = JSONArray.parseArray(json);
            if (root == null) return result;
            for (int i = 0; i < root.size(); i++) {
                JSONArray kline = root.getJSONArray(i);
                if (kline == null || kline.size() < 6) continue;
                try {
                    KlineData k = new KlineData();
                    k.setTimestamp(kline.getLong(0));
                    k.setOpen(kline.getDouble(1));
                    k.setHigh(kline.getDouble(2));
                    k.setLow(kline.getDouble(3));
                    k.setClose(kline.getDouble(4));
                    k.setVolume(kline.getDouble(5));
                    if (kline.size() > 6) k.setCloseTime(kline.getLong(6));
                    if (kline.size() > 7) k.setQuoteVolume(kline.getDouble(7));
                    if (kline.size() > 8) k.setTrades(kline.getLong(8));
                    if (kline.size() > 9) k.setTakerBuyBase(kline.getDouble(9));
                    if (kline.size() > 10) k.setTakerBuyQuote(kline.getDouble(10));
                    result.add(k);
                } catch (Exception e) {
                    log.warn("解析第{}根K线失败", i, e);
                }
            }
        } catch (Exception e) {
            log.error("解析币安JSON响应失败", e);
        }
        return result;
    }

    /**
     * 合并旧数据和新数据（按timestamp去重）
     */
    public List<KlineData> mergeData(List<KlineData> oldData, List<KlineData> newData) {
        if (oldData == null || oldData.isEmpty()) return newData != null ? newData : new ArrayList<>();
        if (newData == null || newData.isEmpty()) return oldData;

        Set<Long> oldTs = new HashSet<>();
        for (KlineData k : oldData) oldTs.add(k.getTimestamp());

        List<KlineData> merged = new ArrayList<>(oldData);
        for (KlineData k : newData) {
            if (!oldTs.contains(k.getTimestamp())) {
                merged.add(k);
            }
        }
        merged.sort(Comparator.comparing(KlineData::getTimestamp));
        log.info("  ✓ 合并: {}+{}={}根", oldData.size(), newData.size(), merged.size());
        return merged;
    }

    /**
     * 裁剪数据至指定数量（保留最新的limit根）
     */
    public List<KlineData> trimToLimit(List<KlineData> data, int limit) {
        if (data == null) return new ArrayList<>();
        if (data.size() <= limit) {
            log.info("  ✓ 数量: {}根（无需裁剪）", data.size());
            return data;
        }
        List<KlineData> trimmed = data.subList(data.size() - limit, data.size());
        // subList返回的是原列表的视图，需要新建ArrayList
        List<KlineData> result = new ArrayList<>(trimmed);
        log.info("  ✓ 裁剪: {}→{}根", data.size(), result.size());
        return result;
    }

    /**
     * 保存数据到CSV
     */
    public void saveData(String symbol, String tf, List<KlineData> data) {
        String path = csvPath(symbol, tf);
        try {
            ensureSymbolDir(symbol);
            try (CSVWriter writer = new CSVWriter(new FileWriter(path))) {
                writer.writeNext(CSV_HEADER);
                for (KlineData k : data) {
                    writer.writeNext(k.toCsvRow());
                }
            }
            log.info("  ✓ 已保存: {}", path);
        } catch (IOException e) {
            log.error("  ✗ 保存失败: {}", path, e);
        }
    }

    /**
     * 更新数据（增量更新或全量拉取）
     * 逻辑与Python版 update_data() 一致：
     * 1. 本地数据充足且较新 → 直接返回
     * 2. 否则增量拉取新数据，合并后裁剪保存
     */
    public List<KlineData> updateData(String symbol, String tf, int limit) {
        List<KlineData> local = loadLocalData(symbol, tf);

        // 本地数据充足且较新 → 直接返回
        if (local != null && local.size() >= limit) {
            long latest = getLatestTimestamp(local);
            long now = System.currentTimeMillis();
            if (now - latest <= 3_600_000L) { // 1小时内
                List<KlineData> trimmed = trimToLimit(local, limit);
                if (trimmed.size() != local.size()) {
                    saveData(symbol, tf, trimmed);
                }
                return trimmed;
            }
        }

        // 需要更新
        log.info("  增量更新...");
        Long startTime = (local != null && !local.isEmpty())
                ? getLatestTimestamp(local) + 1 : null;
        List<KlineData> newData = fetchKlineData(symbol, tf, limit, startTime);

        if (newData == null || newData.isEmpty()) {
            log.info("  增量失败，使用本地数据");
            return trimToLimit(local, limit);
        }

        List<KlineData> merged = (local != null && !local.isEmpty())
                ? mergeData(local, newData) : newData;
        List<KlineData> finalData = trimToLimit(merged, limit);
        saveData(symbol, tf, finalData);
        return finalData;
    }
}
