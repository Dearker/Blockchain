package com.blockchain.sra.core;


import com.blockchain.sra.model.*;

import java.util.*;

/**
 * 斐波那契回调/扩展位计算器
 *
 * 回调位（用于支撑位分析）:
 *   0%, 23.6%, 38.2%, 50%, 61.8%, 78.6%, 100%
 *
 * 扩展位（用于阻力位分析）:
 *   127.2%, 161.8%, 200%, 261.8%
 *
 * 强度判定:
 *   - 回调 38.2%~61.8% → 中（3星）
 *   - 回调 23.6%、78.6% → 弱（2星）
 *   - 扩展 127.2%~161.8% → 中（3星）
 *   - 扩展 100%、200%、261.8% → 弱（2星）
 */
public class FibonacciCalculator {

    /** 斐波那契回调比例 */
    public static final double[] RETRACEMENT_RATIOS = {
            0.0, 0.236, 0.382, 0.5, 0.618, 0.786, 1.0
    };

    /** 斐波那契扩展比例 */
    public static final double[] EXTENSION_RATIOS = {
            1.0, 1.272, 1.618, 2.0, 2.618
    };

    /** 回调比例与强度映射 */
    private static final Map<Double, StrengthLevel> RETRACEMENT_STRENGTH = new LinkedHashMap<>();
    /** 扩展比例与强度映射 */
    private static final Map<Double, StrengthLevel> EXTENSION_STRENGTH = new LinkedHashMap<>();

    static {
        RETRACEMENT_STRENGTH.put(0.0, StrengthLevel.VERY_WEAK);
        RETRACEMENT_STRENGTH.put(0.236, StrengthLevel.WEAK);
        RETRACEMENT_STRENGTH.put(0.382, StrengthLevel.MEDIUM);
        RETRACEMENT_STRENGTH.put(0.5, StrengthLevel.MEDIUM);
        RETRACEMENT_STRENGTH.put(0.618, StrengthLevel.MEDIUM);
        RETRACEMENT_STRENGTH.put(0.786, StrengthLevel.WEAK);
        RETRACEMENT_STRENGTH.put(1.0, StrengthLevel.VERY_WEAK);

        EXTENSION_STRENGTH.put(1.0, StrengthLevel.WEAK);
        EXTENSION_STRENGTH.put(1.272, StrengthLevel.MEDIUM);
        EXTENSION_STRENGTH.put(1.618, StrengthLevel.MEDIUM);
        EXTENSION_STRENGTH.put(2.0, StrengthLevel.WEAK);
        EXTENSION_STRENGTH.put(2.618, StrengthLevel.WEAK);
    }

    /**
     * 计算斐波那契回调位
     * 公式: 回调位 = high - (high - low) * ratio
     *
     * @param high      波段最高价
     * @param low       波段最低价
     * @param timeFrame 时间级别
     * @return 回调位列表
     */
    public static List<Level> calculateRetracement(double high, double low, TimeFrame timeFrame) {
        List<Level> levels = new ArrayList<>();
        double range = high - low;

        for (double ratio : RETRACEMENT_RATIOS) {
            double price = high - range * ratio;
            StrengthLevel strength = RETRACEMENT_STRENGTH.getOrDefault(ratio, StrengthLevel.VERY_WEAK);

            Level level = new Level(price, LevelType.SUPPORT, LevelSource.FIBONACCI_RETRACEMENT, timeFrame);
            level.setStrength(strength.getValue());
            level.setStrengthLabel(strength.getLabel() + " " + strength.getStars());
            levels.add(level);
        }

        return levels;
    }

    /**
     * 计算斐波那契扩展位
     * 公式: 扩展位 = low + (high - low) * ratio
     *
     * @param high      波段最高价
     * @param low       波段最低价
     * @param timeFrame 时间级别
     * @return 扩展位列表
     */
    public static List<Level> calculateExtension(double high, double low, TimeFrame timeFrame) {
        List<Level> levels = new ArrayList<>();
        double range = high - low;

        for (double ratio : EXTENSION_RATIOS) {
            double price = low + range * ratio;
            StrengthLevel strength = EXTENSION_STRENGTH.getOrDefault(ratio, StrengthLevel.VERY_WEAK);

            Level level = new Level(price, LevelType.RESISTANCE, LevelSource.FIBONACCI_EXTENSION, timeFrame);
            level.setStrength(strength.getValue());
            level.setStrengthLabel(strength.getLabel() + " " + strength.getStars());
            levels.add(level);
        }

        return levels;
    }

    /**
     * 自动识别波段高低点并计算斐波那契位
     * 在回溯周期内找最高价和最低价作为波段
     *
     * @param klines    K线数据
     * @param timeFrame 时间级别
     * @return 斐波那契分析结果（回调 + 扩展）
     */
    public static FibonacciResult analyzeWithKlines(List<Kline> klines, TimeFrame timeFrame) {
        if (klines == null || klines.isEmpty()) {
            return new FibonacciResult(Collections.emptyList(), Collections.emptyList(), 0, 0);
        }

        // 找到回溯周期内的最高价和最低价
        double periodHigh = klines.stream().mapToDouble(Kline::getHigh).max().orElse(0);
        double periodLow = klines.stream().mapToDouble(Kline::getLow).min().orElse(0);

        List<Level> retracements = calculateRetracement(periodHigh, periodLow, timeFrame);
        List<Level> extensions = calculateExtension(periodHigh, periodLow, timeFrame);

        return new FibonacciResult(retracements, extensions, periodHigh, periodLow);
    }

    /**
     * 斐波那契计算结果
     */
    public static class FibonacciResult {
        private final List<Level> retracements;
        private final List<Level> extensions;
        private final double fibHigh;
        private final double fibLow;

        public FibonacciResult(List<Level> retracements, List<Level> extensions, double fibHigh, double fibLow) {
            this.retracements = retracements;
            this.extensions = extensions;
            this.fibHigh = fibHigh;
            this.fibLow = fibLow;
        }

        public List<Level> getRetracements() { return retracements; }
        public List<Level> getExtensions() { return extensions; }
        public double getFibHigh() { return fibHigh; }
        public double getFibLow() { return fibLow; }
    }
}
