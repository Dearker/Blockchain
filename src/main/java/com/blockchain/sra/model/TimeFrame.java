package com.blockchain.sra.model;

/**
 * 时间级别枚举
 */
public enum TimeFrame {

    /** 4小时线 - 短期趋势分析，回溯300根 */
    H4("4H", 300, "短期趋势分析"),

    /** 日线 - 中期趋势分析，回溯180根 */
    D1("1D", 180, "中期趋势分析"),

    /** 周线 - 长期趋势分析，回溯100根 */
    W1("1W", 100, "长期趋势分析");

    private final String code;
    private final int lookback;
    private final String description;

    TimeFrame(String code, int lookback, String description) {
        this.code = code;
        this.lookback = lookback;
        this.description = description;
    }

    public String getCode() { return code; }
    public int getLookback() { return lookback; }
    public String getDescription() { return description; }
}
