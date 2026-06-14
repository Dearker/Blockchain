package com.blockchain.sra.model;

/**
 * 强度等级枚举
 */
public enum StrengthLevel {

    VERY_WEAK(1, "极弱", "⭐"),
    WEAK(2, "弱", "⭐⭐"),
    MEDIUM(3, "中", "⭐⭐⭐"),
    STRONG(4, "强", "⭐⭐⭐⭐"),
    VERY_STRONG(5, "极强", "⭐⭐⭐⭐⭐");

    private final int value;
    private final String label;
    private final String stars;

    StrengthLevel(int value, String label, String stars) {
        this.value = value;
        this.label = label;
        this.stars = stars;
    }

    public int getValue() { return value; }
    public String getLabel() { return label; }
    public String getStars() { return stars; }

    public static StrengthLevel fromValue(int value) {
        for (StrengthLevel sl : values()) {
            if (sl.value == value) return sl;
        }
        return VERY_WEAK;
    }
}
