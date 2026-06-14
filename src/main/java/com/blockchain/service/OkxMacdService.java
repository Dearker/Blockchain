package com.blockchain.service;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSONArray;
import com.blockchain.domain.SpotAnalysis;
import com.blockchain.param.SpotQueryParam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.averages.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OkxMacdService {

    private final SpotAnalysisService spotAnalysisService;

    /**
     * 获取 K 线数据并计算每一根 K 线的 MACD 指标
     * @param param 查询参数
     * @return 填充了 MACD 数据的 SpotAnalysis 列表 (按时间正序排列)
     */
    public List<SpotAnalysis> getAnalysisWithMacd(SpotQueryParam param) {
        JSONArray dataArray = spotAnalysisService.getResultData(param);
        if (CollUtil.isEmpty(dataArray)) {
            return Collections.emptyList();
        }
        // 2. 初始化 ta4j 容器
        List<Bar> bars = new ArrayList<>();
        BarSeries series = new BaseBarSeries(param.getInstId(), bars);
        List<SpotAnalysis> analysisList = new ArrayList<>();

        // 根据 bar 参数解析周期
        Duration timePeriod = parseDuration(param.getBar());

        // OKX 原始数据是倒序的 (index 0 是最新)，我们需要正序处理以确保指标计算正确
        for (int i = dataArray.size() - 1; i >= 0; i--) {
            JSONArray k = dataArray.getJSONArray(i);

            // 解析字段
            long beginTs = k.getLong(0);
            BigDecimal open = k.getBigDecimal(1);
            BigDecimal high = k.getBigDecimal(2);
            BigDecimal low = k.getBigDecimal(3);
            BigDecimal close = k.getBigDecimal(4);
            BigDecimal vol = k.getBigDecimal(5);      // 交易量（币）
            BigDecimal volCcy = k.getBigDecimal(6);   // 交易额（USDT）
            int confirm = k.getIntValue(8);

            // 转换时间
            Instant beginTime = Instant.ofEpochMilli(beginTs);
            Instant endTime = beginTime.plus(timePeriod);
            LocalDateTime startLdt = LocalDateTime.ofInstant(beginTime, ZoneId.systemDefault());

            // 修复：使用 ta4j 0.22.4 要求的 10 参数构造函数
            // BaseBar(Duration timePeriod, Instant beginTime, Instant endTime, Num openPrice, Num highPrice, Num lowPrice, Num closePrice, Num volume, Num amount, long trades)
            Bar bar = new BaseBar(
                    timePeriod,
                    beginTime,
                    endTime,
                    DecimalNum.valueOf(open),
                    DecimalNum.valueOf(high),
                    DecimalNum.valueOf(low),
                    DecimalNum.valueOf(close),
                    DecimalNum.valueOf(vol),
                    DecimalNum.valueOf(volCcy),
                    0L // trades 笔数，OKX 基础 K 线不提供，填 0
            );
            series.addBar(bar);

            // 构建基础实体对象
            SpotAnalysis analysis = SpotAnalysis.builder()
                    .startTime(startLdt)
                    .moneyType(param.getInstId())
                    .timeType(param.getBar())
                    .priceOpen(open)
                    .priceHigh(high)
                    .priceLow(low)
                    .priceClose(close)
                    .volume(vol)
                    .volumeCcy(volCcy)
                    .confirm(confirm)
                    .createTime(LocalDateTime.now())
                    .priceDifferent(high.subtract(low))
                    .amplitude(open.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
                            high.subtract(low).divide(open, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")))
                    .priceLimit(open.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
                            close.subtract(open).divide(open, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")))
                    .build();

            analysisList.add(analysis);
        }

        // 3. 计算 MACD 指标
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        MACDIndicator difInd = new MACDIndicator(closePrice, 12, 26);
        EMAIndicator deaInd = new EMAIndicator(difInd, 9);

        // 4. 回填指标值
        for (int i = 0; i < analysisList.size(); i++) {
            SpotAnalysis entity = analysisList.get(i);

            Num difNum = difInd.getValue(i);
            Num deaNum = deaInd.getValue(i);

            // 计算 MACD 柱状值: (DIF - DEA) * 2
            Num macdNum = difNum.minus(deaNum).multipliedBy(DecimalNum.valueOf(2));

            entity.setDif(new BigDecimal(difNum.toString()).setScale(8, RoundingMode.HALF_UP));
            entity.setDea(new BigDecimal(deaNum.toString()).setScale(8, RoundingMode.HALF_UP));
            entity.setMacd(new BigDecimal(macdNum.toString()).setScale(8, RoundingMode.HALF_UP));
        }

        spotAnalysisService.saveBatch(analysisList);
        return analysisList;
    }

    /**
     * 周期转换
     */
    private Duration parseDuration(String bar) {
        if (bar == null) return Duration.ofMinutes(1);
        String lowerBar = bar.toLowerCase();
        if (lowerBar.contains("h")) {
            int h = Integer.parseInt(lowerBar.replace("h", ""));
            return Duration.ofHours(h);
        } else if (lowerBar.contains("d")) {
            int d = Integer.parseInt(lowerBar.replace("d", ""));
            return Duration.ofDays(d);
        } else if (lowerBar.contains("m")) {
            int m = Integer.parseInt(lowerBar.replace("m", ""));
            return Duration.ofMinutes(m);
        } else if (lowerBar.contains("w")) {
            return Duration.ofDays(7);
        }
        return Duration.ofMinutes(1);
    }

}
