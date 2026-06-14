package com.blockchain.sra.model;

import java.util.Objects;

/**
 * 支撑/阻力位数据模型
 */
public class Level {

    /** 价格 */
    private final double price;

    /** 类型：支撑 or 阻力 */
    private final LevelType type;

    /** 来源方式 */
    private final LevelSource source;

    /** 来源时间级别 */
    private final TimeFrame timeFrame;

    /** 强度 1~5 */
    private int strength;

    /** 强度描述 */
    private String strengthLabel;

    /** 测试次数（该价位被触及的次数） */
    private int testCount;

    /** 重叠的时间级别数量 */
    private int overlapCount;

    public Level(double price, LevelType type, LevelSource source, TimeFrame timeFrame) {
        this.price = price;
        this.type = type;
        this.source = source;
        this.timeFrame = timeFrame;
        this.strength = 1;
        this.testCount = 0;
        this.overlapCount = 1;
    }

    // ========== 计算方法 ==========

    /**
     * 判断两个价位是否"相近"（间距 <= threshold%）
     */
    public boolean isNear(Level other, double thresholdPercent) {
        double diff = Math.abs(this.price - other.price) / this.price * 100;
        return diff <= thresholdPercent;
    }

    /**
     * 计算与当前价格的距离百分比
     */
    public double distancePercent(double currentPrice) {
        return (this.price - currentPrice) / currentPrice * 100;
    }

    // ========== Getter & Setter ==========

    public double getPrice() { return price; }
    public LevelType getType() { return type; }
    public LevelSource getSource() { return source; }
    public TimeFrame getTimeFrame() { return timeFrame; }

    public int getStrength() { return strength; }
    public void setStrength(int strength) { this.strength = strength; }

    public String getStrengthLabel() { return strengthLabel; }
    public void setStrengthLabel(String strengthLabel) { this.strengthLabel = strengthLabel; }

    public int getTestCount() { return testCount; }
    public void setTestCount(int testCount) { this.testCount = testCount; }

    public int getOverlapCount() { return overlapCount; }
    public void setOverlapCount(int overlapCount) { this.overlapCount = overlapCount; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Level level = (Level) o;
        return Double.compare(level.price, price) == 0
                && type == level.type
                && source == level.source
                && timeFrame == level.timeFrame;
    }

    @Override
    public int hashCode() {
        return Objects.hash(price, type, source, timeFrame);
    }

    @Override
    public String toString() {
        return String.format("%s | %.2f | 强度:%s(%d星) | 来源:%s | 级别:%s | 测试:%d次 | 重叠:%d级别",
                type.getLabel(), price, strengthLabel, strength, source.getLabel(), timeFrame.getCode(), testCount, overlapCount);
    }
}
