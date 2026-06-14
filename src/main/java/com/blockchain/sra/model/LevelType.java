package com.blockchain.sra.model;

/**
 * 支撑/阻力位类型
 */
public enum LevelType {

    SUPPORT("支撑位", "S"),
    RESISTANCE("阻力位", "R");

    private final String label;
    private final String code;

    LevelType(String label, String code) {
        this.label = label;
        this.code = code;
    }

    public String getLabel() { return label; }
    public String getCode() { return code; }
}
