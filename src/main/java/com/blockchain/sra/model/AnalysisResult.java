package com.blockchain.sra.model;

import java.util.*;

/**
 * 多级别综合分析结果
 */
public class AnalysisResult {

    /** 交易对名称 */
    private String symbol;

    /** 当前价格 */
    private double currentPrice;

    /** 分析时间 */
    private String analysisTime;

    /** 各时间级别分析结果 */
    private final Map<TimeFrame, TimeFrameAnalysis> frameAnalyses = new LinkedHashMap<>();

    /** 多级别重叠的支撑位 */
    private final List<Level> overlappedSupports = new ArrayList<>();

    /** 多级别重叠的阻力位 */
    private final List<Level> overlappedResistances = new ArrayList<>();

    /** 三层防御体系 - 支撑 */
    private final Map<String, Level> defenseSupportLayers = new LinkedHashMap<>();

    /** 三层防御体系 - 阻力 */
    private final Map<String, Level> defenseResistanceLayers = new LinkedHashMap<>();

    public void putFrameAnalysis(TimeFrame tf, TimeFrameAnalysis analysis) {
        frameAnalyses.put(tf, analysis);
    }

    public TimeFrameAnalysis getFrameAnalysis(TimeFrame tf) {
        return frameAnalyses.get(tf);
    }

    // ========== Getter & Setter ==========

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public String getAnalysisTime() { return analysisTime; }
    public void setAnalysisTime(String analysisTime) { this.analysisTime = analysisTime; }

    public Map<TimeFrame, TimeFrameAnalysis> getFrameAnalyses() { return frameAnalyses; }

    public List<Level> getOverlappedSupports() { return overlappedSupports; }
    public List<Level> getOverlappedResistances() { return overlappedResistances; }

    public Map<String, Level> getDefenseSupportLayers() { return defenseSupportLayers; }
    public Map<String, Level> getDefenseResistanceLayers() { return defenseResistanceLayers; }
}
