package com.blockchain.analysis.service;

import com.blockchain.analysis.model.AnalysisResult;
import com.blockchain.analysis.model.KlineData;
import com.blockchain.analysis.model.PriceLevel;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 支撑阻力位分析核心类
 * 对应Python版 SupportResistanceAnalyzer
 *
 * 分析方法：
 * 1. 斐波那契回调位（Fibonacci Retracement）
 * 2. 斐波那契扩展位（Fibonacci Extension）
 * 3. 历史局部高低点
 * 4. 最近显著高低点
 */
@Slf4j
//@Service
public class SupportResistanceAnalyzer {

    private final String symbol;
    private final String timeframe;
    private final List<KlineData> data;
    private final int window;

    // 预处理数据
    private final List<Double> highs;
    private final List<Double> lows;
    private final List<Double> closes;
    private final double currentPrice;
    private final LocalDateTime currentTime;
    private final double recentHigh;
    private final double recentLow;
    private final LocalDateTime highTime;
    private final LocalDateTime lowTime;

    public SupportResistanceAnalyzer(String symbol, String timeframe,
                                    List<KlineData> data, int window) {
        this.symbol = symbol;
        this.timeframe = timeframe;
        this.data = data;
        this.window = window;

        this.highs = new ArrayList<>();
        this.lows = new ArrayList<>();
        this.closes = new ArrayList<>();
        for (KlineData k : data) {
            this.highs.add(k.getHigh());
            this.lows.add(k.getLow());
            this.closes.add(k.getClose());
        }

        this.currentPrice = this.closes.get(this.closes.size() - 1);
        this.currentTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(data.get(data.size() - 1).getTimestamp()),
                ZoneId.systemDefault());

        this.recentHigh = Collections.max(this.highs);
        this.recentLow = Collections.min(this.lows);

        int highIdx = this.highs.indexOf(this.recentHigh);
        int lowIdx = this.lows.indexOf(this.recentLow);
        this.highTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(data.get(highIdx).getTimestamp()),
                ZoneId.systemDefault());
        this.lowTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(data.get(lowIdx).getTimestamp()),
                ZoneId.systemDefault());
    }

    // ==================== 斐波那契回调位 ====================

    /**
     * 计算斐波那契回调位
     * 从最高价向最低价回撤
     */
    public Map<String, Double> fibRetracement() {
        double diff = recentHigh - recentLow;
        Map<String, Double> fib = new LinkedHashMap<>();
        fib.put("23.6%", recentHigh - diff * 0.236);
        fib.put("38.2%", recentHigh - diff * 0.382);
        fib.put("50%",   recentHigh - diff * 0.5);
        fib.put("61.8%", recentHigh - diff * 0.618);
        fib.put("78.6%", recentHigh - diff * 0.786);
        fib.put("100%",  recentLow);
        return fib;
    }

    // ==================== 斐波那契扩展位 ====================

    /**
     * 计算斐波那契扩展位
     * 从最高价向上扩展
     */
    public Map<String, Double> fibExtension() {
        double diff = recentHigh - recentLow;
        Map<String, Double> fib = new LinkedHashMap<>();
        fib.put("100%",   recentHigh);
        fib.put("127.2%", recentHigh + diff * 0.272);
        fib.put("161.8%", recentHigh + diff * 0.618);
        fib.put("200%",   recentHigh + diff);
        return fib;
    }

    // ==================== 局部低点（支撑） ====================

    /**
     * 寻找历史局部低点（窗口法）
     * 条件：当前点是最低价，且价格低于当前价
     */
    private List<PriceLevel> findLocalLows() {
        List<PriceLevel> result = new ArrayList<>();
        for (int i = window; i < data.size() - window; i++) {
            double lowI = lows.get(i);
            boolean isLocalMin = true;
            for (int j = i - window; j <= i + window; j++) {
                if (lows.get(j) < lowI) {
                    isLocalMin = false;
                    break;
                }
            }
            if (isLocalMin && lowI < currentPrice) {
                PriceLevel pl = new PriceLevel();
                pl.setPrice(lowI);
                pl.setTime(LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(data.get(i).getTimestamp()),
                        ZoneId.systemDefault()));
                pl.setDistance((currentPrice - lowI) / currentPrice * 100);
                result.add(pl);
            }
        }
        return result;
    }

    // ==================== 局部高点（阻力） ====================

    /**
     * 寻找历史局部高点（窗口法）
     * 条件：当前点是最高价，且价格高于当前价
     */
    private List<PriceLevel> findLocalHighs() {
        List<PriceLevel> result = new ArrayList<>();
        for (int i = window; i < data.size() - window; i++) {
            double highI = highs.get(i);
            boolean isLocalMax = true;
            for (int j = i - window; j <= i + window; j++) {
                if (highs.get(j) > highI) {
                    isLocalMax = false;
                    break;
                }
            }
            if (isLocalMax && highI > currentPrice) {
                PriceLevel pl = new PriceLevel();
                pl.setPrice(highI);
                pl.setTime(LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(data.get(i).getTimestamp()),
                        ZoneId.systemDefault()));
                pl.setDistance((highI - currentPrice) / currentPrice * 100);
                result.add(pl);
            }
        }
        return result;
    }

    // ==================== 支撑位分析 ====================

    public List<PriceLevel> analyzeSupports() {
        List<PriceLevel> supports = new ArrayList<>();

        // 1. 斐波那契回调位（只保留 < 当前价）
        Map<String, Double> fibRet = fibRetracement();
        for (Map.Entry<String, Double> entry : fibRet.entrySet()) {
            double price = entry.getValue();
            if (price >= currentPrice) continue;
            double distance = (currentPrice - price) / currentPrice * 100;

            PriceLevel pl = new PriceLevel();
            pl.setPrice(price);
            pl.setDistance(distance);
            pl.setType("斐波那契" + entry.getKey() + "回调位");

            // 强度评级
            if ("100%".equals(entry.getKey())) {
                pl.setStrength("★★★★★ 极强（最近低点）");
            } else if (Set.of("38.2%", "50%", "61.8%").contains(entry.getKey())) {
                pl.setStrength("★★★☆☆ 强");
            } else if ("78.6%".equals(entry.getKey())) {
                pl.setStrength("★★☆☆☆ 中");
            } else {
                pl.setStrength("★☆☆☆☆ 弱");
            }
            pl.setLevel(entry.getKey());
            supports.add(pl);
        }

        // 2. 最近显著低点
        if (recentLow < currentPrice) {
            PriceLevel pl = new PriceLevel();
            pl.setPrice(recentLow);
            pl.setDistance((currentPrice - recentLow) / currentPrice * 100);
            pl.setType("最近显著低点");
            pl.setStrength("★★★★★ 极强");
            pl.setTime(lowTime);
            pl.setTested(true);
            supports.add(pl);
        }

        // 3. 历史局部低点
        for (PriceLevel low : findLocalLows()) {
            PriceLevel pl = new PriceLevel();
            pl.setPrice(low.getPrice());
            pl.setDistance(low.getDistance());
            pl.setType("历史局部低点");
            pl.setStrength("★☆☆☆☆ 弱");
            pl.setTime(low.getTime());
            supports.add(pl);
        }

        // 按距离排序
        supports.sort(Comparator.comparing(PriceLevel::getDistance));
        return supports;
    }

    // ==================== 阻力位分析 ====================

    public List<PriceLevel> analyzeResistances() {
        List<PriceLevel> resistances = new ArrayList<>();

        // 1. 斐波那契回调位（只保留 > 当前价）
        Map<String, Double> fibRet = fibRetracement();
        for (Map.Entry<String, Double> entry : fibRet.entrySet()) {
            double price = entry.getValue();
            if (price <= currentPrice) continue;
            double distance = (price - currentPrice) / currentPrice * 100;

            PriceLevel pl = new PriceLevel();
            pl.setPrice(price);
            pl.setDistance(distance);
            pl.setType("斐波那契" + entry.getKey() + "回调位");
            pl.setStrength("23.6%".equals(entry.getKey()) ? "★★☆☆☆ 中" : "★★★☆☆ 强");
            pl.setLevel(entry.getKey());
            resistances.add(pl);
        }

        // 2. 斐波那契扩展位
        Map<String, Double> fibExt = fibExtension();
        for (Map.Entry<String, Double> entry : fibExt.entrySet()) {
            double price = entry.getValue();
            if (price <= currentPrice) continue;
            double distance = (price - currentPrice) / currentPrice * 100;

            PriceLevel pl = new PriceLevel();
            pl.setPrice(price);
            pl.setDistance(distance);
            pl.setType("斐波那契" + entry.getKey() + "扩展位");

            if ("100%".equals(entry.getKey())) {
                pl.setStrength("★★★★★ 极强（最近高点）");
            } else if ("161.8%".equals(entry.getKey())) {
                pl.setStrength("★★★☆☆ 强");
            } else {
                pl.setStrength("★★☆☆☆ 中");
            }
            pl.setLevel(entry.getKey());
            resistances.add(pl);
        }

        // 3. 最近显著高点
        if (recentHigh > currentPrice) {
            PriceLevel pl = new PriceLevel();
            pl.setPrice(recentHigh);
            pl.setDistance((recentHigh - currentPrice) / currentPrice * 100);
            pl.setType("最近显著高点");
            pl.setStrength("★★★★★ 极强");
            pl.setTime(highTime);
            pl.setTested(true);
            resistances.add(pl);
        }

        // 4. 历史局部高点
        for (PriceLevel high : findLocalHighs()) {
            PriceLevel pl = new PriceLevel();
            pl.setPrice(high.getPrice());
            pl.setDistance(high.getDistance());
            pl.setType("历史局部高点");
            pl.setStrength("★☆☆☆☆ 弱");
            pl.setTime(high.getTime());
            resistances.add(pl);
        }

        resistances.sort(Comparator.comparing(PriceLevel::getDistance));
        return resistances;
    }

    // ==================== 生成分析结果 ====================

    public AnalysisResult generateAnalysisResult() {
        List<PriceLevel> supports = analyzeSupports();
        List<PriceLevel> resistances = analyzeResistances();

        AnalysisResult result = new AnalysisResult();
        result.setSymbol(symbol);
        result.setTimeframe(timeframe);
        result.setKlineCount(data.size());
        result.setCurrentPrice(currentPrice);
        result.setCurrentTime(currentTime);
        result.setRecentHigh(recentHigh);
        result.setRecentLow(recentLow);
        result.setHighTime(highTime);
        result.setLowTime(lowTime);
        result.setRangePct((recentHigh - recentLow) / recentLow * 100);
        result.setFibRetracement(fibRetracement());
        result.setFibExtension(fibExtension());
        result.setSupports(supports.size() > 10 ? supports.subList(0, 10) : supports);
        result.setResistances(resistances.size() > 10 ? resistances.subList(0, 10) : resistances);
        result.setSupportsCount(supports.size());
        result.setResistancesCount(resistances.size());
        return result;
    }
}
