package com.blockchain.sra.model;

import java.time.LocalDateTime;

/**
 * K线数据模型
 * CSV文件需包含: 日期, 开盘价, 最高价, 最低价, 收盘价, 成交量
 */
public class Kline {

    /** 时间戳 */
    private LocalDateTime datetime;

    /** 开盘价 */
    private double open;

    /** 最高价 */
    private double high;

    /** 最低价 */
    private double low;

    /** 收盘价 */
    private double close;

    /** 成交量 */
    private double volume;

    public Kline() {}

    public Kline(LocalDateTime datetime, double open, double high, double low, double close, double volume) {
        this.datetime = datetime;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    // ========== Getter & Setter ==========

    public LocalDateTime getDatetime() { return datetime; }
    public void setDatetime(LocalDateTime datetime) { this.datetime = datetime; }

    public double getOpen() { return open; }
    public void setOpen(double open) { this.open = open; }

    public double getHigh() { return high; }
    public void setHigh(double high) { this.high = high; }

    public double getLow() { return low; }
    public void setLow(double low) { this.low = low; }

    public double getClose() { return close; }
    public void setClose(double close) { this.close = close; }

    public double getVolume() { return volume; }
    public void setVolume(double volume) { this.volume = volume; }

    @Override
    public String toString() {
        return String.format("Kline{datetime=%s, open=%.2f, high=%.2f, low=%.2f, close=%.2f, volume=%.0f}",
                datetime, open, high, low, close, volume);
    }
}
