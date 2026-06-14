package com.blockchain.service.impl;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blockchain.constants.BinanceApiConstants;
import com.blockchain.dao.KlineDataMapper;
import com.blockchain.dao.KlineSyncRecordMapper;
import com.blockchain.domain.KlineData;
import com.blockchain.domain.KlineSyncRecord;
import com.blockchain.service.KlineDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * K线数据MySQL存储服务实现类
 * 使用MyBatis-Plus简化数据库操作
 *
 * @author blockchain
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KlineDataServiceImpl extends ServiceImpl<KlineDataMapper, KlineData> implements KlineDataService {

    private final KlineSyncRecordMapper syncRecordMapper;

    @Override
    public void syncKlineData(String symbol) {
        log.info("开始同步K线数据到MySQL: {}", symbol);
        for (String interval : BinanceApiConstants.SUPPORTED_INTERVALS) {
            syncInterval(symbol, interval, BinanceApiConstants.INTERVAL_TARGET_COUNT.get(interval));
        }
        log.info("MySQL同步完成: {}", symbol);
    }

    @Override
    public void syncKlineData(List<String> symbols) {
        symbols.forEach(this::syncKlineData);
    }

    private void syncInterval(String symbol, String interval, int targetCount) {
        KlineSyncRecord record = getSyncRecord(symbol, interval);

        List<KlineData> newData;
        if (record == null) {
            newData = fetchFullData(symbol, interval, targetCount);
        } else {
            newData = fetchIncrementalData(symbol, interval, record.getLastOpenTime());
        }

        if (newData.isEmpty()) {
            log.info("无新数据: {} {}", symbol, interval);
            return;
        }

        saveBatch(newData, 500);
        cleanOldData(symbol, interval, targetCount);
        updateSyncRecord(symbol, interval, targetCount);
        log.info("同步完成: {} {} {}条", symbol, interval, newData.size());
    }

    private KlineSyncRecord getSyncRecord(String symbol, String interval) {
        return syncRecordMapper.selectOne(new LambdaQueryWrapper<KlineSyncRecord>()
                .eq(KlineSyncRecord::getSymbol, symbol)
                .eq(KlineSyncRecord::getTimeInterval, interval)
                .eq(KlineSyncRecord::getStorageType, BinanceApiConstants.STORAGE_TYPE_MYSQL));
    }

    private List<KlineData> fetchFullData(String symbol, String interval, int targetCount) {
        List<KlineData> allData = new ArrayList<>();
        Long endTime = null;
        while (allData.size() < targetCount) {
            int limit = Math.min(BinanceApiConstants.API_MAX_LIMIT, targetCount - allData.size());
            List<KlineData> batch = fetchFromApi(symbol, interval, null, endTime, limit);
            if (batch.isEmpty()) break;
            allData.addAll(batch);
            endTime = batch.get(batch.size() - 1).getOpenTime() - 1;
        }
        return allData.stream().distinct().toList();
    }

    private List<KlineData> fetchIncrementalData(String symbol, String interval, Long startTime) {
        return fetchFromApi(symbol, interval, startTime, null, BinanceApiConstants.API_MAX_LIMIT);
    }

    private List<KlineData> fetchFromApi(String symbol, String interval, Long startTime, Long endTime, int limit) {
        String url = buildUrl(symbol, interval, startTime, endTime, limit);
        log.debug("请求API: {}", url);

        try {
            JSONArray array = JSON.parseArray(HttpUtil.get(url, 30000));
            List<KlineData> result = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (int i = 0; i < array.size(); i++) {
                JSONArray arr = array.getJSONArray(i);
                result.add(KlineData.builder()
                        .symbol(symbol).timeInterval(interval)
                        .openTime(arr.getLong(0)).openPrice(arr.getBigDecimal(1))
                        .highPrice(arr.getBigDecimal(2)).lowPrice(arr.getBigDecimal(3))
                        .closePrice(arr.getBigDecimal(4)).volume(arr.getBigDecimal(5))
                        .closeTime(arr.getLong(6)).quoteAssetVolume(arr.getBigDecimal(7))
                        .numberOfTrades(arr.getInteger(8))
                        .takerBuyBaseAssetVolume(arr.getBigDecimal(9))
                        .takerBuyQuoteAssetVolume(arr.getBigDecimal(10))
                        .createTime(now).updateTime(now).build());
            }
            return result.stream().distinct().toList();
        } catch (Exception e) {
            log.error("拉取失败: {}", url, e);
            throw new RuntimeException("拉取币安K线数据失败", e);
        }
    }

    private String buildUrl(String symbol, String interval, Long startTime, Long endTime, int limit) {
        StringBuilder sb = new StringBuilder(BinanceApiConstants.KLINES_API_URL);
        sb.append("?symbol=").append(symbol).append("&interval=").append(interval).append("&limit=").append(limit);
        if (startTime != null) sb.append("&startTime=").append(startTime);
        if (endTime != null) sb.append("&endTime=").append(endTime);
        return sb.toString();
    }

    private void cleanOldData(String symbol, String interval, int keepCount) {
        long total = count(new LambdaQueryWrapper<KlineData>()
                .eq(KlineData::getSymbol, symbol).eq(KlineData::getTimeInterval, interval));
        if (total <= keepCount) return;

        List<KlineData> keepList = list(new LambdaQueryWrapper<KlineData>()
                .eq(KlineData::getSymbol, symbol).eq(KlineData::getTimeInterval, interval)
                .orderByDesc(KlineData::getOpenTime).last("LIMIT " + keepCount));

        if (!keepList.isEmpty()) {
            remove(new LambdaQueryWrapper<KlineData>()
                    .eq(KlineData::getSymbol, symbol).eq(KlineData::getTimeInterval, interval)
                    .lt(KlineData::getOpenTime, keepList.get(keepList.size() - 1).getOpenTime()));
        }
    }

    private void updateSyncRecord(String symbol, String interval, int targetCount) {
        KlineData latest = getOne(new LambdaQueryWrapper<KlineData>()
                .eq(KlineData::getSymbol, symbol).eq(KlineData::getTimeInterval, interval)
                .orderByDesc(KlineData::getOpenTime).last("LIMIT 1"));
        if (latest == null) return;

        LocalDateTime now = LocalDateTime.now();
        int currentCount = (int) count(new LambdaQueryWrapper<KlineData>()
                .eq(KlineData::getSymbol, symbol).eq(KlineData::getTimeInterval, interval));

        KlineSyncRecord record = getSyncRecord(symbol, interval);
        if (record == null) {
            syncRecordMapper.insert(KlineSyncRecord.builder()
                    .symbol(symbol).timeInterval(interval)
                    .storageType(BinanceApiConstants.STORAGE_TYPE_MYSQL)
                    .lastOpenTime(latest.getOpenTime()).lastCloseTime(latest.getCloseTime())
                    .currentCount(currentCount).targetCount(targetCount)
                    .lastSyncTime(now).createTime(now).updateTime(now).build());
        } else {
            syncRecordMapper.update(null, new LambdaUpdateWrapper<KlineSyncRecord>()
                    .eq(KlineSyncRecord::getId, record.getId())
                    .set(KlineSyncRecord::getLastOpenTime, latest.getOpenTime())
                    .set(KlineSyncRecord::getLastCloseTime, latest.getCloseTime())
                    .set(KlineSyncRecord::getCurrentCount, currentCount)
                    .set(KlineSyncRecord::getLastSyncTime, now)
                    .set(KlineSyncRecord::getUpdateTime, now));
        }
    }
}
