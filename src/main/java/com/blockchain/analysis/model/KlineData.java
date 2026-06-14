package com.blockchain.analysis.model;

import lombok.Data;

/**
 * K线数据模型（对应币安合约K线API返回字段）
 * 原始CSV列: timestamp, open, high, low, close, volume, close_time, quote_volume, trades, taker_buy_base, taker_buy_quote, ignore
 */
@Data
public class KlineData {
    /** 开盘时间戳 (ms) */
    private Long timestamp;
    /** 开盘价 */
    private Double open;
    /** 最高价 */
    private Double high;
    /** 最低价 */
    private Double low;
    /** 收盘价 */
    private Double close;
    /** 成交量 */
    private Double volume;
    /** 收盘时间戳 (ms) */
    private Long closeTime;
    /** 报价成交量 */
    private Double quoteVolume;
    /** 成交笔数 */
    private Long trades;
    /** 主动买入成交量 */
    private Double takerBuyBase;
    /** 主动买入成交额 */
    private Double takerBuyQuote;
    /** 忽略字段 */
    private String ignore;

    public static KlineData fromCsvRow(String[] row) {
        KlineData k = new KlineData();
        k.setTimestamp(row.length > 0 ? Long.parseLong(row[0]) : 0L);
        k.setOpen(row.length > 1 ? Double.parseDouble(row[1]) : 0.0);
        k.setHigh(row.length > 2 ? Double.parseDouble(row[2]) : 0.0);
        k.setLow(row.length > 3 ? Double.parseDouble(row[3]) : 0.0);
        k.setClose(row.length > 4 ? Double.parseDouble(row[4]) : 0.0);
        k.setVolume(row.length > 5 ? Double.parseDouble(row[5]) : 0.0);
        k.setCloseTime(row.length > 6 ? Long.parseLong(row[6]) : 0L);
        k.setQuoteVolume(row.length > 7 ? Double.parseDouble(row[7]) : 0.0);
        k.setTrades(row.length > 8 ? Long.parseLong(row[8]) : 0L);
        k.setTakerBuyBase(row.length > 9 ? Double.parseDouble(row[9]) : 0.0);
        k.setTakerBuyQuote(row.length > 10 ? Double.parseDouble(row[10]) : 0.0);
        k.setIgnore(row.length > 11 ? row[11] : "");
        return k;
    }

    public String[] toCsvRow() {
        return new String[]{
            String.valueOf(timestamp),
            String.valueOf(open),
            String.valueOf(high),
            String.valueOf(low),
            String.valueOf(close),
            String.valueOf(volume),
            String.valueOf(closeTime),
            String.valueOf(quoteVolume),
            String.valueOf(trades),
            String.valueOf(takerBuyBase),
            String.valueOf(takerBuyQuote),
            ignore != null ? ignore : ""
        };
    }

    public Double getHigh() { return high; }
    public Double getLow() { return low; }
    public Double getClose() { return close; }
    public Long getTimestamp() { return timestamp; }
}
