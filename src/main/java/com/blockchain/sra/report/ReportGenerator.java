package com.blockchain.sra.report;

import com.blockchain.sra.model.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 分析报告生成器
 * 输出Markdown格式报告
 */
public class ReportGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static String repeat(String s, int count) {
        StringBuilder sb = new StringBuilder(s.length() * count);
        for (int i = 0; i < count; i++) sb.append(s);
        return sb.toString();
    }

    /**
     * 生成控制台报告
     */
    public static String generateConsoleReport(AnalysisResult result) {
        StringBuilder sb = new StringBuilder();

        // ========== 标题 ==========
        sb.append(repeat("═", 60)).append("\n");
        sb.append("  多时间级别支撑阻力位分析报告\n");
        sb.append(repeat("═", 60)).append("\n\n");

        // ========== 数据概览 ==========
        sb.append("【数据概览】\n");
        sb.append(String.format("  交易对: %s\n", result.getSymbol()));
        sb.append(String.format("  当前价格: %.2f\n", result.getCurrentPrice()));
        sb.append(String.format("  分析时间: %s\n", result.getAnalysisTime()));
        sb.append("\n");

        // ========== 各级别分析 ==========
        for (Map.Entry<TimeFrame, TimeFrameAnalysis> entry : result.getFrameAnalyses().entrySet()) {
            TimeFrame tf = entry.getKey();
            TimeFrameAnalysis analysis = entry.getValue();

            sb.append(repeat("─", 50)).append("\n");
            sb.append(String.format("【%s 级别分析】 %s\n", tf.getCode(), tf.getDescription()));
            sb.append(repeat("─", 50)).append("\n");
            sb.append(String.format("  趋势: %s\n", analysis.getTrend()));
            sb.append(String.format("  区间: %.2f ~ %.2f\n", analysis.getPeriodLow(), analysis.getPeriodHigh()));
            sb.append(String.format("  斐波那契基准: High=%.2f, Low=%.2f\n", analysis.getFibHigh(), analysis.getFibLow()));
            sb.append("\n");

            // 支撑位
            sb.append("  ▼ 支撑位:\n");
            if (analysis.getSupportLevels().isEmpty()) {
                sb.append("    (无)\n");
            } else {
                for (Level s : analysis.getSupportLevels()) {
                    sb.append(formatLevelLine(s, result.getCurrentPrice()));
                }
            }
            sb.append("\n");

            // 阻力位
            sb.append("  ▲ 阻力位:\n");
            if (analysis.getResistanceLevels().isEmpty()) {
                sb.append("    (无)\n");
            } else {
                for (Level r : analysis.getResistanceLevels()) {
                    sb.append(formatLevelLine(r, result.getCurrentPrice()));
                }
            }
            sb.append("\n");
        }

        // ========== 多级别重叠 ==========
        sb.append(repeat("─", 50)).append("\n");
        sb.append("【多级别重叠分析】\n");
        sb.append(repeat("─", 50)).append("\n");

        sb.append("  重叠支撑位:\n");
        if (result.getOverlappedSupports().isEmpty()) {
            sb.append("    (无)\n");
        } else {
            for (Level s : result.getOverlappedSupports()) {
                sb.append(formatLevelLine(s, result.getCurrentPrice()));
            }
        }

        sb.append("  重叠阻力位:\n");
        if (result.getOverlappedResistances().isEmpty()) {
            sb.append("    (无)\n");
        } else {
            for (Level r : result.getOverlappedResistances()) {
                sb.append(formatLevelLine(r, result.getCurrentPrice()));
            }
        }
        sb.append("\n");

        // ========== 三层防御体系 ==========
        sb.append(repeat("─", 50)).append("\n");
        sb.append("【三层防御体系】\n");
        sb.append(repeat("─", 50)).append("\n");

        sb.append("  ▼ 支撑防线:\n");
        for (Map.Entry<String, Level> e : result.getDefenseSupportLayers().entrySet()) {
            sb.append(formatDefenseLine(e.getKey(), e.getValue(), result.getCurrentPrice()));
        }

        sb.append("  ▲ 阻力防线:\n");
        for (Map.Entry<String, Level> e : result.getDefenseResistanceLayers().entrySet()) {
            sb.append(formatDefenseLine(e.getKey(), e.getValue(), result.getCurrentPrice()));
        }

        sb.append("\n").append(repeat("═", 60)).append("\n");

        return sb.toString();
    }

    /**
     * 生成Markdown报告并保存到文件
     */
    public static void generateMarkdownReport(AnalysisResult result, String outputPath) throws IOException {
        String md = generateMarkdownContent(result);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            writer.write(md);
        }
    }

    /**
     * 生成Markdown格式内容
     */
    public static String generateMarkdownContent(AnalysisResult result) {
        StringBuilder sb = new StringBuilder();

        sb.append("# 多时间级别支撑阻力位分析报告\n\n");

        // 数据概览
        sb.append("## 1. 实时数据概览\n\n");
        sb.append("| 项目 | 值 |\n");
        sb.append("|------|----|\n");
        sb.append(String.format("| 交易对 | %s |\n", result.getSymbol()));
        sb.append(String.format("| 当前价格 | %.2f |\n", result.getCurrentPrice()));
        sb.append(String.format("| 分析时间 | %s |\n\n", result.getAnalysisTime()));

        // 各级别分析
        for (Map.Entry<TimeFrame, TimeFrameAnalysis> entry : result.getFrameAnalyses().entrySet()) {
            TimeFrame tf = entry.getKey();
            TimeFrameAnalysis analysis = entry.getValue();

            sb.append(String.format("## %s 级别分析（%s）\n\n", tf.getCode(), tf.getDescription()));
            sb.append(String.format("- **趋势**: %s\n", analysis.getTrend()));
            sb.append(String.format("- **区间**: %.2f ~ %.2f\n", analysis.getPeriodLow(), analysis.getPeriodHigh()));
            sb.append(String.format("- **斐波那契基准**: High=%.2f, Low=%.2f\n\n", analysis.getFibHigh(), analysis.getFibLow()));

            // 支撑位表
            sb.append("### 支撑位\n\n");
            if (analysis.getSupportLevels().isEmpty()) {
                sb.append("*（无显著支撑位）*\n\n");
            } else {
                sb.append("| 价格 | 距离 | 强度 | 来源 | 测试次数 | 重叠级别 |\n");
                sb.append("|------|------|------|------|----------|----------|\n");
                for (Level s : analysis.getSupportLevels()) {
                    sb.append(formatLevelRow(s, result.getCurrentPrice()));
                }
                sb.append("\n");
            }

            // 阻力位表
            sb.append("### 阻力位\n\n");
            if (analysis.getResistanceLevels().isEmpty()) {
                sb.append("*（无显著阻力位）*\n\n");
            } else {
                sb.append("| 价格 | 距离 | 强度 | 来源 | 测试次数 | 重叠级别 |\n");
                sb.append("|------|------|------|------|----------|----------|\n");
                for (Level r : analysis.getResistanceLevels()) {
                    sb.append(formatLevelRow(r, result.getCurrentPrice()));
                }
                sb.append("\n");
            }
        }

        // 多级别重叠
        sb.append("## 多级别对比分析\n\n");
        sb.append("### 重叠支撑位\n\n");
        if (result.getOverlappedSupports().isEmpty()) {
            sb.append("*（无多级别重叠支撑位）*\n\n");
        } else {
            sb.append("| 价格 | 距离 | 强度 | 重叠级别数 |\n");
            sb.append("|------|------|------|------------|\n");
            for (Level s : result.getOverlappedSupports()) {
                sb.append(String.format("| %.2f | %.2f%% | %s | %d |\n",
                        s.getPrice(), s.distancePercent(result.getCurrentPrice()), s.getStrengthLabel(), s.getOverlapCount()));
            }
            sb.append("\n");
        }

        sb.append("### 重叠阻力位\n\n");
        if (result.getOverlappedResistances().isEmpty()) {
            sb.append("*（无多级别重叠阻力位）*\n\n");
        } else {
            sb.append("| 价格 | 距离 | 强度 | 重叠级别数 |\n");
            sb.append("|------|------|------|------------|\n");
            for (Level r : result.getOverlappedResistances()) {
                sb.append(String.format("| %.2f | %.2f%% | %s | %d |\n",
                        r.getPrice(), r.distancePercent(result.getCurrentPrice()), r.getStrengthLabel(), r.getOverlapCount()));
            }
            sb.append("\n");
        }

        // 三层防御体系
        sb.append("## 支撑/阻力位层级分析\n\n");

        sb.append("### 三层支撑防线\n\n");
        if (result.getDefenseSupportLayers().isEmpty()) {
            sb.append("*（数据不足以构建支撑防线）*\n\n");
        } else {
            sb.append("| 层级 | 价格 | 距离 | 强度 |\n");
            sb.append("|------|------|------|------|\n");
            for (Map.Entry<String, Level> e : result.getDefenseSupportLayers().entrySet()) {
                sb.append(String.format("| %s | %.2f | %.2f%% | %s |\n",
                        e.getKey(), e.getValue().getPrice(), e.getValue().distancePercent(result.getCurrentPrice()), e.getValue().getStrengthLabel()));
            }
            sb.append("\n");
        }

        sb.append("### 三层阻力防线\n\n");
        if (result.getDefenseResistanceLayers().isEmpty()) {
            sb.append("*（数据不足以构建阻力防线）*\n\n");
        } else {
            sb.append("| 层级 | 价格 | 距离 | 强度 |\n");
            sb.append("|------|------|------|------|\n");
            for (Map.Entry<String, Level> e : result.getDefenseResistanceLayers().entrySet()) {
                sb.append(String.format("| %s | %.2f | %.2f%% | %s |\n",
                        e.getKey(), e.getValue().getPrice(), e.getValue().distancePercent(result.getCurrentPrice()), e.getValue().getStrengthLabel()));
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    // ========== 格式化辅助方法 ==========

    private static String formatLevelLine(Level level, double currentPrice) {
        double dist = level.distancePercent(currentPrice);
        String arrow = level.getType() == LevelType.SUPPORT ? "↓" : "↑";
        return String.format("    %s %.2f  |  距离: %.2f%%  |  %s  |  来源: %s  |  测试: %d次  |  重叠: %d级别\n",
                arrow, level.getPrice(), dist, level.getStrengthLabel(), level.getSource().getLabel(),
                level.getTestCount(), level.getOverlapCount());
    }

    private static String formatDefenseLine(String layerName, Level level, double currentPrice) {
        double dist = level.distancePercent(currentPrice);
        return String.format("    [%s] %.2f  |  距离: %.2f%%  |  %s\n",
                layerName, level.getPrice(), dist, level.getStrengthLabel());
    }

    private static String formatLevelRow(Level level, double currentPrice) {
        return String.format("| %.2f | %.2f%% | %s | %s | %d | %d |\n",
                level.getPrice(), level.distancePercent(currentPrice), level.getStrengthLabel(),
                level.getSource().getLabel(), level.getTestCount(), level.getOverlapCount());
    }
}
