package com.blockchain.analysis.service;

import com.blockchain.analysis.model.AnalysisResult;
import com.blockchain.analysis.model.DefenseLayer;
import com.blockchain.analysis.model.OverlappingLevel;
import com.blockchain.analysis.model.PriceLevel;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Markdown 报告生成器
 * 对应Python版 generate_report() 函数
 */
@Slf4j
//@Service
public class ReportGenerator {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 生成完整的多级别支撑阻力位分析报告（Markdown格式）
     */
    public String generateReport(String symbol, Map<String, AnalysisResult> results) {
        StringBuilder sb = new StringBuilder();
        appendHeader(sb, symbol, results);
        appendOverview(sb, results);
        appendSingleTimeframeAnalysis(sb, results);
        appendOverlappingLevels(sb, results);
        appendDefenseLayers(sb, results);
        appendTradingStrategy(sb, results);
        return sb.toString();
    }

    // ==================== 报告头 ====================

    private void appendHeader(StringBuilder sb, String symbol,
                              Map<String, AnalysisResult> results) {
        sb.append("# ").append(symbol).append(" 多时间级别支撑阻力位分析报告\n\n");
        sb.append("**分析时间**: ").append(LocalDateTime.now().format(DT_FMT)).append("\n");
        sb.append("**数据来源**: 币安API（增量CSV数据）\n");
        sb.append("**分析方法**: 斐波那契回调/扩展 + 历史高低点 + 多级别对比\n");
        sb.append("**核心原则**: ✅ 支撑<当前价 | ✅ 阻力>当前价\n\n");
        sb.append("---\n\n");
    }

    // ==================== 一、数据概览 ====================

    private void appendOverview(StringBuilder sb,
                                Map<String, AnalysisResult> results) {
        sb.append("## 一、数据概览\n\n");
        sb.append("| 时间级别 | 要求 | 实际 | 状态 | 当前价 | 最高价 | 最低价 |\n");
        sb.append("|----------|------|------|------|--------|--------|--------|\n");

        // TIMEFRAMES配置（与Python版一致）
        Map<String, Integer> timeFrameLimits = Map.of("4h", 300, "1d", 180, "1w", 100);

        for (String tf : timeFrameLimits.keySet()) {
            if (!results.containsKey(tf)) continue;
            AnalysisResult r = results.get(tf);
            int limit = timeFrameLimits.get(tf);
            String status = (r.getKlineCount() != null && r.getKlineCount() >= limit)
                    ? "✅ 符合" : "⚠️ 不足";
            sb.append("| **").append(tf.toUpperCase()).append("** | ")
                    .append(limit).append("根 | ")
                    .append(r.getKlineCount()).append("根 | ")
                    .append(status).append(" ")
                    .append("| **$").append(formatPrice(r.getCurrentPrice())).append("** | ")
                    .append("$").append(formatPrice(r.getRecentHigh())).append(" | ")
                    .append("$").append(formatPrice(r.getRecentLow())).append(" |\n");
        }
        sb.append("\n---\n\n");
    }

    // ==================== 二、单级别分析 ====================

    private void appendSingleTimeframeAnalysis(StringBuilder sb,
                                              Map<String, AnalysisResult> results) {
        for (String tf : results.keySet()) {
            AnalysisResult r = results.get(tf);
            sb.append("## 二、").append(tf.toUpperCase()).append("级别分析\n\n");
            sb.append("**当前价**: $").append(formatPrice(r.getCurrentPrice())).append(")  ")
                    .append("|  **高点**: $").append(formatPrice(r.getRecentHigh())).append("(")
                    .append(r.getHighTime() != null ? r.getHighTime().format(DATE_FMT) : "").append(")  ")
                    .append("|  **低点**: $").append(formatPrice(r.getRecentLow())).append("(")
                    .append(r.getLowTime() != null ? r.getLowTime().format(DATE_FMT) : "").append(")\n\n");

            // 支撑位
            sb.append("### 支撑位（< $").append(formatPrice(r.getCurrentPrice())).append("）\n\n");
            sb.append("| 序号 | 价格 | 距离 | 类型 | 强度 |\n");
            sb.append("|------|------|------|------|------|\n");
            if (r.getSupports() != null) {
                int idx = 1;
                for (PriceLevel s : r.getSupports()) {
                    sb.append("| S").append(idx++).append(" | $")
                            .append(formatPrice(s.getPrice())).append(" | -")
                            .append(formatPct(s.getDistance())).append("% | ")
                            .append(s.getType() != null ? s.getType() : "").append(" | ")
                            .append(s.getStrength() != null ? s.getStrength() : "").append(" |\n");
                }
            }
            sb.append("\n");

            // 阻力位
            sb.append("### 阻力位（> $").append(formatPrice(r.getCurrentPrice())).append("）\n\n");
            sb.append("| 序号 | 价格 | 距离 | 类型 | 强度 |\n");
            sb.append("|------|------|------|------|------|\n");
            if (r.getResistances() != null) {
                int idx = 1;
                for (PriceLevel rs : r.getResistances()) {
                    sb.append("| R").append(idx++).append(" | $")
                            .append(formatPrice(rs.getPrice())).append(" | +")
                            .append(formatPct(rs.getDistance())).append("% | ")
                            .append(rs.getType() != null ? rs.getType() : "").append(" | ")
                            .append(rs.getStrength() != null ? rs.getStrength() : "").append(" |\n");
                }
            }
            sb.append("\n---\n\n");
        }
    }

    // ==================== 三、多级别重叠 ====================

    private void appendOverlappingLevels(StringBuilder sb,
                                        Map<String, AnalysisResult> results) {
        sb.append("## 三、多级别重叠 ★★★★★\n\n");

        List<PriceLevel> allSupports = new ArrayList<>();
        List<PriceLevel> allResistances = new ArrayList<>();
        new MultiTimeframeAnalyzer().collectAllLevels(results, allSupports, allResistances);

        // 重叠支撑位
        MultiTimeframeAnalyzer mta = new MultiTimeframeAnalyzer();
        List<OverlappingLevel> overlappingS =
                mta.findOverlappingLevels(allSupports, 0.02);

        if (!overlappingS.isEmpty()) {
            sb.append("### 重叠支撑位\n\n");
            sb.append("| 价格 | 重叠级别 | 强度 |\n");
            sb.append("|------|----------|--------|\n");
            for (OverlappingLevel o : overlappingS.stream().limit(5).toList()) {
                sb.append("| **$").append(formatPrice(o.getPrice())).append("** | ")
                        .append(String.join("+", o.getTimeframes())).append(" | ")
                        .append(o.getStrength()).append(" |\n");
            }
        } else {
            sb.append("暂未发现多级别重叠支撑位\n");
        }
        sb.append("\n");

        // 重叠阻力位
        List<OverlappingLevel> overlappingR =
                mta.findOverlappingLevels(allResistances, 0.02);

        if (!overlappingR.isEmpty()) {
            sb.append("### 重叠阻力位\n\n");
            sb.append("| 价格 | 重叠级别 | 强度 |\n");
            sb.append("|------|----------|--------|\n");
            for (OverlappingLevel o : overlappingR.stream().limit(5).toList()) {
                sb.append("| **$").append(formatPrice(o.getPrice())).append("** | ")
                        .append(String.join("+", o.getTimeframes())).append(" | ")
                        .append(o.getStrength()).append(" |\n");
            }
        } else {
            sb.append("暂未发现多级别重叠阻力位\n");
        }
        sb.append("\n---\n\n");
    }

    // ==================== 四、三层防御体系 ====================

    private void appendDefenseLayers(StringBuilder sb,
                                     Map<String, AnalysisResult> results) {
        sb.append("## 四、三层防御体系\n\n");

        List<PriceLevel> allSupports = new ArrayList<>();
        List<PriceLevel> allResistances = new ArrayList<>();
        MultiTimeframeAnalyzer mta = new MultiTimeframeAnalyzer();
        mta.collectAllLevels(results, allSupports, allResistances);

        Map<String, DefenseLayer> dl = mta.classifyDefenseLayers(allSupports, allResistances);

        appendDefenseLayer(sb, dl, "layer1", "第一层（短期±1-3%）");
        appendDefenseLayer(sb, dl, "layer2", "第二层（中期±3-10%）");
        appendDefenseLayer(sb, dl, "layer3", "第三层（长期±10%以上）");

        sb.append("---\n\n");
    }

    private void appendDefenseLayer(StringBuilder sb,
                                    Map<String, DefenseLayer> dl,
                                    String key, String label) {
        sb.append("### ").append(label).append("\n\n");
        DefenseLayer layer = dl.get(key);
        if (layer != null) {
            if (layer.getSupports() != null && !layer.getSupports().isEmpty()) {
                PriceLevel s = layer.getSupports().get(0);
                sb.append("- 支撑: $").append(formatPrice(s.getPrice()))
                        .append(" (-").append(formatPct(s.getDistance())).append("%) [")
                        .append(s.getTimeframe() != null ? s.getTimeframe().toUpperCase() : "").append("]\n");
            }
            if (layer.getResistances() != null && !layer.getResistances().isEmpty()) {
                PriceLevel r = layer.getResistances().get(0);
                sb.append("- 阻力: $").append(formatPrice(r.getPrice()))
                        .append(" (+").append(formatPct(r.getDistance())).append("%) [")
                        .append(r.getTimeframe() != null ? r.getTimeframe().toUpperCase() : "").append("]\n");
            }
        }
        sb.append("\n");
    }

    // ==================== 五、交易策略 ====================

    private void appendTradingStrategy(StringBuilder sb,
                                       Map<String, AnalysisResult> results) {
        sb.append("## 五、交易策略建议\n\n");

        if (results.containsKey("4h")) {
            List<PriceLevel> allSupports = new ArrayList<>();
            List<PriceLevel> allResistances = new ArrayList<>();
            MultiTimeframeAnalyzer mta = new MultiTimeframeAnalyzer();
            mta.collectAllLevels(results, allSupports, allResistances);
            Map<String, DefenseLayer> dl = mta.classifyDefenseLayers(allSupports, allResistances);

            DefenseLayer layer1 = dl.get("layer1");
            if (layer1 != null && layer1.getSupports() != null && !layer1.getSupports().isEmpty()) {
                PriceLevel s1 = layer1.getSupports().get(0);
                sb.append("- **回调至 $").append(formatPrice(s1.getPrice())).append(" 支撑**: 轻仓做多，止损 $")
                        .append(formatPrice(s1.getPrice() * 0.98)).append("\n");
            }
            if (layer1 != null && layer1.getResistances() != null && !layer1.getResistances().isEmpty()) {
                PriceLevel r1 = layer1.getResistances().get(0);
                sb.append("- **反弹至 $").append(formatPrice(r1.getPrice())).append(" 阻力**: 轻仓做空，止损 $")
                        .append(formatPrice(r1.getPrice() * 1.02)).append("\n");
            }
        }
        sb.append("\n⚠️ **本分析仅供参考，不构成投资建议。**\n\n");
        sb.append("**分析机构**: Hermes Agent 自然交易分析系统\n");
    }

    // ==================== 工具方法 ====================

    private String formatPrice(double price) {
        return String.format("%,.2f", price);
    }

    private String formatPct(double pct) {
        return String.format("%.2f", pct);
    }
}
