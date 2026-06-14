package com.blockchain.analysis.model;

import lombok.Data;
import java.util.List;

/**
 * 多级别重叠价位
 */
@Data
public class OverlappingLevel {
    /** 重叠均价 */
    private Double price;
    /** 重叠的时间级别列表 */
    private List<String> timeframes;
    /** 类型列表 */
    private List<String> types;
    /** 重叠次数 */
    private Integer overlapCount;
    /** 强度评级 */
    private String strength;
}
