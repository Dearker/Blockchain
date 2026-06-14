package com.blockchain.analysis.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 支撑位或阻力位模型
 */
@Data
public class PriceLevel {
    /** 价格 */
    private Double price;
    /** 距离当前价的百分比距离（正数） */
    private Double distance;
    /** 类型描述（如：斐波那契38.2%回调位、历史局部低点） */
    private String type;
    /** 强度评级（如：★★★★★ 极强） */
    private String strength;
    /** 级别标识（如：23.6%、38.2% 等，可选） */
    private String level;
    /** 时间级别（4h/1d/1w，用于多级别重叠分析） */
    private String timeframe;
    /** 出现时间 */
    private LocalDateTime time;
    /** 是否已测试 */
    private Boolean tested;

    public PriceLevel() {
        this.tested = false;
    }
}
