package com.blockchain.sra.model;

/**
 * 支撑/阻力位来源
 */
public enum LevelSource {

    FIBONACCI_RETRACEMENT("斐波那契回调", "FibRet"),
    FIBONACCI_EXTENSION("斐波那契扩展", "FibExt"),
    LOCAL_LOW("历史局部低点", "LocalLow"),
    LOCAL_HIGH("历史局部高点", "LocalHigh"),
    RECENT_SIGNIFICANT_LOW("最近显著低点", "RecentSigLow"),
    RECENT_SIGNIFICANT_HIGH("最近显著高点", "RecentSigHigh");

    private final String label;
    private final String code;

    LevelSource(String label, String code) {
        this.label = label;
        this.code = code;
    }

    public String getLabel() { return label; }
    public String getCode() { return code; }
}
