package com.blockchain.sra.core;


import com.blockchain.sra.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 单级别分析引擎
 *
 * 分析步骤:
 * 1. 计算斐波那契回调位（支撑）
 * 2. 计算斐波那契扩展位（阻力）
 * 3. 识别历史局部高低点
 * 4. 识别最近显著高低点
 * 5. 过滤：支撑位 < 当前价格，阻力位 > 当前价格（使用统一当前价格）
 * 6. 评估强度
 * 7. 合并相近位（间距 > 1%）
 */
public class SingleTimeFrameAnalyzer {

    /**
     * 执行单级别完整分析
     *
     * @param klines       该时间级别的K线数据
     * @param timeFrame    时间级别
     * @param currentPrice 统一当前价格（跨所有时间级别一致）
     * @return 该级别的分析结果
     */
    public static TimeFrameAnalysis analyze(List<Kline> klines, TimeFrame timeFrame, double currentPrice) {
        TimeFrameAnalysis analysis = new TimeFrameAnalysis(timeFrame);

        if (klines == null || klines.isEmpty()) {
            return analysis;
        }

        double periodHigh = klines.stream().mapToDouble(Kline::getHigh).max().orElse(0);
        double periodLow = klines.stream().mapToDouble(Kline::getLow).min().orElse(0);

        analysis.setPeriodHigh(periodHigh);
        analysis.setPeriodLow(periodLow);

        // ========== Step 1: 斐波那契回调/扩展 ==========
        FibonacciCalculator.FibonacciResult fibResult = FibonacciCalculator.analyzeWithKlines(klines, timeFrame);
        analysis.setFibHigh(fibResult.getFibHigh());
        analysis.setFibLow(fibResult.getFibLow());

        // ========== Step 2: 历史局部高低点 ==========
        List<Double> localHighs = LocalExtremumFinder.findLocalHighs(klines, timeFrame);
        List<Double> localLows = LocalExtremumFinder.findLocalLows(klines, timeFrame);

        // ========== Step 3: 最近显著高低点 ==========
        double recentHigh = LocalExtremumFinder.findRecentSignificantHigh(klines);
        double recentLow = LocalExtremumFinder.findRecentSignificantLow(klines);

        // ========== Step 4: 构建支撑位列表 ==========
        List<Level> supports = new ArrayList<>();

        // 4a. 斐波那契回调位（作为支撑）- 跳过0%（=high，无意义）
        for (Level fibLevel : fibResult.getRetracements()) {
            if (fibLevel.getPrice() < currentPrice && fibLevel.getPrice() > periodLow) {
                supports.add(fibLevel);
            }
        }

        // 4b. 历史局部低点（作为支撑）
        for (Double low : localLows) {
            if (low < currentPrice) {
                Level level = new Level(low, LevelType.SUPPORT, LevelSource.LOCAL_LOW, timeFrame);
                level.setStrength(StrengthLevel.VERY_WEAK.getValue());
                level.setStrengthLabel(StrengthLevel.VERY_WEAK.getLabel() + " " + StrengthLevel.VERY_WEAK.getStars());
                supports.add(level);
            }
        }

        // 4c. 最近显著低点（作为支撑）
        if (recentLow < currentPrice) {
            Level level = new Level(recentLow, LevelType.SUPPORT, LevelSource.RECENT_SIGNIFICANT_LOW, timeFrame);
            level.setStrength(StrengthLevel.STRONG.getValue());
            level.setStrengthLabel(StrengthLevel.STRONG.getLabel() + " " + StrengthLevel.STRONG.getStars());
            supports.add(level);
        }

        // ========== Step 5: 构建阻力位列表 ==========
        List<Level> resistances = new ArrayList<>();

        // 5a. 斐波那契扩展位（作为阻力）- 跳过100%（=high，和最近显著高点重复）
        for (Level fibLevel : fibResult.getExtensions()) {
            if (fibLevel.getPrice() > currentPrice && fibLevel.getPrice() > periodHigh) {
                resistances.add(fibLevel);
            }
        }

        // 5b. 历史局部高点（作为阻力）
        for (Double high : localHighs) {
            if (high > currentPrice) {
                Level level = new Level(high, LevelType.RESISTANCE, LevelSource.LOCAL_HIGH, timeFrame);
                level.setStrength(StrengthLevel.VERY_WEAK.getValue());
                level.setStrengthLabel(StrengthLevel.VERY_WEAK.getLabel() + " " + StrengthLevel.VERY_WEAK.getStars());
                resistances.add(level);
            }
        }

        // 5c. 最近显著高点（作为阻力）
        if (recentHigh > currentPrice) {
            Level level = new Level(recentHigh, LevelType.RESISTANCE, LevelSource.RECENT_SIGNIFICANT_HIGH, timeFrame);
            level.setStrength(StrengthLevel.STRONG.getValue());
            level.setStrengthLabel(StrengthLevel.STRONG.getLabel() + " " + StrengthLevel.STRONG.getStars());
            resistances.add(level);
        }

        // ========== Step 6: 评估强度 ==========
        for (Level s : supports) {
            StrengthEvaluator.evaluateSupportStrength(s, klines, 1);
        }
        for (Level r : resistances) {
            StrengthEvaluator.evaluateResistanceStrength(r, klines, 1);
        }

        // ========== Step 7: 合并相近位 ==========
        supports = MultiTimeFrameAnalyzer.mergeCloseLevels(supports);
        resistances = MultiTimeFrameAnalyzer.mergeCloseLevels(resistances);

        // ========== Step 8: 添加到分析结果 ==========
        for (Level s : supports) {
            analysis.addSupportLevel(s);
        }
        for (Level r : resistances) {
            analysis.addResistanceLevel(r);
        }

        // ========== Step 9: 趋势判断 ==========
        analysis.setTrend(determineTrend(klines));

        return analysis;
    }

    /**
     * 简单趋势判断
     * 使用短期MA vs 长期MA
     */
    private static String determineTrend(List<Kline> klines) {
        if (klines.size() < 20) return "数据不足";

        double maShort = calculateMA(klines, 10);
        double maLong = calculateMA(klines, 20);
        double currentPrice = klines.get(klines.size() - 1).getClose();

        if (maShort > maLong && currentPrice > maShort) {
            return "上涨";
        } else if (maShort < maLong && currentPrice < maShort) {
            return "下跌";
        } else {
            return "震荡";
        }
    }

    /**
     * 计算简单移动平均线
     */
    private static double calculateMA(List<Kline> klines, int period) {
        if (klines.size() < period) return 0;
        double sum = 0;
        for (int i = klines.size() - period; i < klines.size(); i++) {
            sum += klines.get(i).getClose();
        }
        return sum / period;
    }
}
