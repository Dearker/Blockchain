package com.blockchain.sra.core;


import com.blockchain.sra.model.Kline;
import com.blockchain.sra.model.LevelType;
import com.blockchain.sra.model.TimeFrame;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 局部高低点识别器
 *
 * 算法逻辑:
 * 1. 历史局部高点: 某根K线的最高价 > 左右各N根K线的最高价
 * 2. 历史局部低点: 某根K线的最低价 < 左右各N根K线的最低价
 * 3. 最近显著高点: 最近N根K线内的最高价
 * 4. 最近显著低点: 最近N根K线内的最低价
 *
 * 窗口大小根据时间级别自动调整
 */
public class LocalExtremumFinder {

    /** 不同时间级别的局部极值窗口大小 */
    private static final Map<TimeFrame, Integer> WINDOW_SIZES = new EnumMap<>(TimeFrame.class);

    static {
        WINDOW_SIZES.put(TimeFrame.H4, 5);   // 4H线: 左右各5根
        WINDOW_SIZES.put(TimeFrame.D1, 5);   // 日线: 左右各5根
        WINDOW_SIZES.put(TimeFrame.W1, 3);    // 周线: 左右各3根
    }

    /** "最近显著"的回看根数 */
    private static final int RECENT_BARS = 20;

    /**
     * 识别历史局部高点
     *
     * @param klines    K线数据
     * @param timeFrame 时间级别
     * @return 局部高点价格列表（按时间升序）
     */
    public static List<Double> findLocalHighs(List<Kline> klines, TimeFrame timeFrame) {
        List<Double> highs = new ArrayList<>();
        int window = WINDOW_SIZES.getOrDefault(timeFrame, 5);

        for (int i = window; i < klines.size() - window; i++) {
            if (isLocalHigh(klines, i, window)) {
                highs.add(klines.get(i).getHigh());
            }
        }

        return highs;
    }

    /**
     * 识别历史局部低点
     *
     * @param klines    K线数据
     * @param timeFrame 时间级别
     * @return 局部低点价格列表（按时间升序）
     */
    public static List<Double> findLocalLows(List<Kline> klines, TimeFrame timeFrame) {
        List<Double> lows = new ArrayList<>();
        int window = WINDOW_SIZES.getOrDefault(timeFrame, 5);

        for (int i = window; i < klines.size() - window; i++) {
            if (isLocalLow(klines, i, window)) {
                lows.add(klines.get(i).getLow());
            }
        }

        return lows;
    }

    /**
     * 获取最近显著高点
     *
     * @param klines K线数据
     * @return 最近显著高点价格
     */
    public static double findRecentSignificantHigh(List<Kline> klines) {
        int start = Math.max(0, klines.size() - RECENT_BARS);
        double maxHigh = Double.MIN_VALUE;
        for (int i = start; i < klines.size(); i++) {
            maxHigh = Math.max(maxHigh, klines.get(i).getHigh());
        }
        return maxHigh;
    }

    /**
     * 获取最近显著低点
     *
     * @param klines K线数据
     * @return 最近显著低点价格
     */
    public static double findRecentSignificantLow(List<Kline> klines) {
        int start = Math.max(0, klines.size() - RECENT_BARS);
        double minLow = Double.MAX_VALUE;
        for (int i = start; i < klines.size(); i++) {
            minLow = Math.min(minLow, klines.get(i).getLow());
        }
        return minLow;
    }

    /**
     * 计算某价位被测试的次数
     * 测试定义: K线的low <= supportPrice * (1 + tolerance) 或 K线的high >= resistancePrice * (1 - tolerance)
     *
     * @param klines    K线数据
     * @param price     待测价位
     * @param type      支撑 or 阻力
     * @param tolerance 容差百分比 (如 0.005 = 0.5%)
     * @return 测试次数
     */
    public static int countTests(List<Kline> klines, double price, LevelType type, double tolerance) {
        int count = 0;
        for (Kline k : klines) {
            if (type == LevelType.SUPPORT) {
                // 支撑位测试: 价格触及该位附近后反弹
                if (k.getLow() <= price * (1 + tolerance) && k.getClose() > price * (1 - tolerance)) {
                    count++;
                }
            } else {
                // 阻力位测试: 价格触及该位附近后回落
                if (k.getHigh() >= price * (1 - tolerance) && k.getClose() < price * (1 + tolerance)) {
                    count++;
                }
            }
        }
        return Math.max(0, count - 1); // 减去形成该位的那一次
    }

    // ========== 私有方法 ==========

    /**
     * 判断是否为局部高点
     */
    private static boolean isLocalHigh(List<Kline> klines, int index, int window) {
        double currentHigh = klines.get(index).getHigh();
        for (int i = index - window; i <= index + window; i++) {
            if (i == index) continue;
            if (klines.get(i).getHigh() >= currentHigh) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断是否为局部低点
     */
    private static boolean isLocalLow(List<Kline> klines, int index, int window) {
        double currentLow = klines.get(index).getLow();
        for (int i = index - window; i <= index + window; i++) {
            if (i == index) continue;
            if (klines.get(i).getLow() <= currentLow) {
                return false;
            }
        }
        return true;
    }
}
