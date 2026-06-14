package com.blockchain.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.blockchain.domain.SpotAnalysis;
import com.blockchain.domain.SpotAnalysisHistory;
import com.blockchain.param.SpotQueryParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class BinanceService {

    /**
     * 调用币安 K线接口，将返回结果解析为 SpotAnalysis 对象列表
     */
    public List<SpotAnalysis> getSpotAnalysisData(SpotQueryParam spotQueryParam) {


        // 币安返回的数据格式为二维数组：
        // [开盘时间, 开盘价, 最高价, 最低价, 收盘价, 交易量, 收盘时间, 交易额, ...]
        JSONArray jsonArray = this.getResultData(spotQueryParam);

        List<SpotAnalysis> klines = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < jsonArray.size(); i++) {
            JSONArray klineData = jsonArray.getJSONArray(i);
            // 提取基础数据
            long openTimeMs = ((Number) klineData.get(0)).longValue();
            BigDecimal open = new BigDecimal(klineData.get(1).toString());
            BigDecimal high = new BigDecimal(klineData.get(2).toString());
            BigDecimal low = new BigDecimal(klineData.get(3).toString());
            BigDecimal close = new BigDecimal(klineData.get(4).toString());
            BigDecimal volume = new BigDecimal(klineData.get(5).toString());
            BigDecimal volumeCcy = new BigDecimal(klineData.get(7).toString()); // 交易额 USDT

            // 计算衍生指标
            BigDecimal priceDifferent = high.subtract(low); // 价差

            // 防止除以0异常（理论上开盘价不会为0，但为严谨起见作处理）
            BigDecimal amplitude = BigDecimal.ZERO;
            BigDecimal priceLimit = BigDecimal.ZERO;
            if (open.compareTo(BigDecimal.ZERO) != 0) {
                // 振幅: (最高 - 最低) / 开盘价
                amplitude = priceDifferent.divide(open, 6, RoundingMode.HALF_UP);
                // 涨跌幅: (收盘 - 开盘) / 开盘价
                priceLimit = close.subtract(open).divide(open, 6, RoundingMode.HALF_UP);
            }

            // 构建 SpotAnalysis 实体
            SpotAnalysis analysis = SpotAnalysis.builder()
                    .startTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(openTimeMs), ZoneId.systemDefault()))
                    .moneyType(spotQueryParam.getInstId())
                    .timeType(spotQueryParam.getBar())
                    .priceOpen(open)
                    .priceHigh(high)
                    .priceLow(low)
                    .priceClose(close)
                    .volume(volume)
                    .volumeCcy(volumeCcy)
                    .priceDifferent(priceDifferent)
                    .amplitude(amplitude)
                    .priceLimit(priceLimit)
                    .createTime(now)
                    .confirm(1) // 此处默认为1(已完结)，如果你需要精确判断最后一根K线是否未完结，可以加时间对比逻辑
                    .build();

            klines.add(analysis);
        }

        return klines;
    }

    private JSONArray getResultData(SpotQueryParam spotQueryParam) {
        Map<String, Object> stringObjectMap = BeanUtil.beanToMap(spotQueryParam);
        List<String> paramList = new ArrayList<>();

        stringObjectMap.forEach((k, v) -> {
            if (Objects.nonNull(v)) {
                paramList.add(k + "=" + v);
            }
        });

        String paramString = String.join("&", paramList);

        String bar = spotQueryParam.getBar();
        String instId = spotQueryParam.getInstId();
        String limit = spotQueryParam.getLimit();
        String url = String.format("https://api.binance.com/api/v3/klines?symbol=%s&interval=%s&limit=%d",
                instId, bar, limit);
        String ok = "https://www.okx.com";

        Map<String, String> paramMap = new HashMap<>();
        /*paramMap.put("apiKey", apiKey);
        paramMap.put("passphrase", apiPassphrase);
        paramMap.put("sign", apiSecretKey);*/
        paramMap.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));

        String body = HttpUtil.createGet(url).addHeaders(paramMap).execute().body();
        return JSON.parseObject(body).getJSONArray("data");
    }

    /**
     * 调用币安 K线接口，将返回结果解析为 SpotAnalysisHistory 对象列表
     */
    public List<SpotAnalysisHistory> getSpotAnalysisHistoryData(SpotQueryParam spotQueryParam) {


        // 币安返回的数据格式为二维数组：
        // [开盘时间, 开盘价, 最高价, 最低价, 收盘价, 交易量, 收盘时间, 交易额, ...]
        JSONArray jsonArray = this.getResultData(spotQueryParam);

        List<SpotAnalysisHistory> klines = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < jsonArray.size(); i++) {
            JSONArray klineData = jsonArray.getJSONArray(i);
            // 提取基础数据
            long openTimeMs = ((Number) klineData.get(0)).longValue();
            BigDecimal open = new BigDecimal(klineData.get(1).toString());
            BigDecimal high = new BigDecimal(klineData.get(2).toString());
            BigDecimal low = new BigDecimal(klineData.get(3).toString());
            BigDecimal close = new BigDecimal(klineData.get(4).toString());
            BigDecimal volume = new BigDecimal(klineData.get(5).toString());
            BigDecimal volumeCcy = new BigDecimal(klineData.get(7).toString()); // 交易额 USDT

            // 计算衍生指标
            BigDecimal priceDifferent = high.subtract(low); // 价差

            // 防止除以0异常（理论上开盘价不会为0，但为严谨起见作处理）
            BigDecimal amplitude = BigDecimal.ZERO;
            BigDecimal priceLimit = BigDecimal.ZERO;
            if (open.compareTo(BigDecimal.ZERO) != 0) {
                // 振幅: (最高 - 最低) / 开盘价
                amplitude = priceDifferent.divide(open, 6, RoundingMode.HALF_UP);
                // 涨跌幅: (收盘 - 开盘) / 开盘价
                priceLimit = close.subtract(open).divide(open, 6, RoundingMode.HALF_UP);
            }

            // 构建 SpotAnalysisHistory 实体
            SpotAnalysisHistory analysis = SpotAnalysisHistory.builder()
                    .startTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(openTimeMs), ZoneId.systemDefault()))
                    .moneyType(spotQueryParam.getInstId())
                    .timeType(spotQueryParam.getBar())
                    .priceOpen(open)
                    .priceHigh(high)
                    .priceLow(low)
                    .priceClose(close)
                    .volume(volume)
                    .volumeCcy(volumeCcy)
                    .priceDifferent(priceDifferent)
                    .amplitude(amplitude)
                    .priceLimit(priceLimit)
                    .createTime(now)
                    .confirm(1) // 此处默认为1(已完结)，如果你需要精确判断最后一根K线是否未完结，可以加时间对比逻辑
                    .build();

            klines.add(analysis);
        }

        return klines;
    }

}
