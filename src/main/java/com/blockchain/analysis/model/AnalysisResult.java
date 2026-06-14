package com.blockchain.analysis.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 单时间级别分析结果
 */
@Data
public class AnalysisResult {
    /** 交易对符号 */
    private String symbol;
    /** 时间级别（4h/1d/1w） */
    private String timeframe;
    /** K线数量 */
    private Integer klineCount;
    /** 当前价格 */
    private Double currentPrice;
    /** 分析时间 */
    private LocalDateTime currentTime;
    /** 区间内最高价 */
    private Double recentHigh;
    /** 区间内最低价 */
    private Double recentLow;
    /** 最高价出现时间 */
    private LocalDateTime highTime;
    /** 最低价出现时间 */
    private LocalDateTime lowTime;
    /** 波动幅度百分比 */
    private Double rangePct;
    /** 斐波那契回调位 map */
    private Map<String, Double> fibRetracement;
    /** 斐波那契扩展位 map */
    private Map<String, Double> fibExtension;
    /** 支撑位列表（前10） */
    private List<PriceLevel> supports;
    /** 阻力位列表（前10） */
    private List<PriceLevel> resistances;
    /** 支撑位总数 */
    private Integer supportsCount;
    /** 阻力位总数 */
    private Integer resistancesCount;
}
