package com.blockchain.sra.model;

import java.util.*;

/**
 * 单个时间级别的分析结果
 */
public class TimeFrameAnalysis {

    private final TimeFrame timeFrame;

    /** 该级别的趋势：上涨/下跌/震荡 */
    private String trend;

    /** 该级别K线数据的最高价 */
    private double periodHigh;

    /** 该级别K线数据的最低价 */
    private double periodLow;

    /** 斐波那契回调位的起点（波段高点） */
    private double fibHigh;

    /** 斐波那契回调位的终点（波段低点） */
    private double fibLow;

    /** 支撑位列表（价格从高到低） */
    private final List<Level> supportLevels = new ArrayList<>();

    /** 阻力位列表（价格从低到高） */
    private final List<Level> resistanceLevels = new ArrayList<>();

    public TimeFrameAnalysis(TimeFrame timeFrame) {
        this.timeFrame = timeFrame;
    }

    /** 添加支撑位并按价格降序排列 */
    public void addSupportLevel(Level level) {
        supportLevels.add(level);
        supportLevels.sort((a, b) -> Double.compare(b.getPrice(), a.getPrice()));
    }

    /** 添加阻力位并按价格升序排列 */
    public void addResistanceLevel(Level level) {
        resistanceLevels.add(level);
        resistanceLevels.sort((a, b) -> Double.compare(a.getPrice(), b.getPrice()));
    }

    /** 获取所有支撑/阻力位 */
    public List<Level> getAllLevels() {
        List<Level> all = new ArrayList<>();
        all.addAll(supportLevels);
        all.addAll(resistanceLevels);
        return all;
    }

    // ========== Getter & Setter ==========

    public TimeFrame getTimeFrame() { return timeFrame; }

    public String getTrend() { return trend; }
    public void setTrend(String trend) { this.trend = trend; }

    public double getPeriodHigh() { return periodHigh; }
    public void setPeriodHigh(double periodHigh) { this.periodHigh = periodHigh; }

    public double getPeriodLow() { return periodLow; }
    public void setPeriodLow(double periodLow) { this.periodLow = periodLow; }

    public double getFibHigh() { return fibHigh; }
    public void setFibHigh(double fibHigh) { this.fibHigh = fibHigh; }

    public double getFibLow() { return fibLow; }
    public void setFibLow(double fibLow) { this.fibLow = fibLow; }

    public List<Level> getSupportLevels() { return supportLevels; }
    public List<Level> getResistanceLevels() { return resistanceLevels; }
}
