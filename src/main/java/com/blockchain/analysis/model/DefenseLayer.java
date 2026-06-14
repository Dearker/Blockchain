package com.blockchain.analysis.model;

import lombok.Data;
import java.util.List;

/**
 * 三层防御体系中的单层
 */
@Data
public class DefenseLayer {
    /** 支撑位列表 */
    private List<PriceLevel> supports;
    /** 阻力位列表 */
    private List<PriceLevel> resistances;
}
