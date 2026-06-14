package com.blockchain.sra.core;


import com.blockchain.sra.model.*;

import java.util.*;

/**
 * 多级别对比分析器
 *
 * 核心逻辑:
 * 1. 汇总各时间级别的支撑/阻力位
 * 2. 检测跨级别重叠（价位间距 <= 1%，且来自不同时间级别）
 * 3. 更新重叠位的强度
 * 4. 建立三层防御体系:
 *    - 短期支撑/阻力（4H级别为主，离当前价最近的有效位）
 *    - 中期支撑/阻力（1D级别为主，离当前价最近的有效位）
 *    - 长期支撑/阻力（1W级别为主，离当前价最近的有效位）
 *    - 优先使用多级别重叠位
 * 5. 合并相近的支撑/阻力位（间距 > 1% 才保留）
 */
public class MultiTimeFrameAnalyzer {

    /** 重叠判断的容差百分比 */
    private static final double OVERLAP_TOLERANCE_PERCENT = 1.0;

    /** 相近价位合并的最小间距百分比 */
    private static final double MERGE_MIN_GAP_PERCENT = 1.0;

    /**
     * 多级别对比分析
     *
     * @param frameAnalyses 各时间级别的分析结果
     * @param currentPrice  统一当前价格（用于验证支撑/阻力位方向）
     * @return 综合分析结果
     */
    public static AnalysisResult analyze(Map<TimeFrame, TimeFrameAnalysis> frameAnalyses, double currentPrice) {
        AnalysisResult result = new AnalysisResult();

        // 1. 收集所有支撑位和阻力位
        List<Level> allSupports = new ArrayList<>();
        List<Level> allResistances = new ArrayList<>();

        for (Map.Entry<TimeFrame, TimeFrameAnalysis> entry : frameAnalyses.entrySet()) {
            TimeFrameAnalysis analysis = entry.getValue();
            allSupports.addAll(analysis.getSupportLevels());
            allResistances.addAll(analysis.getResistanceLevels());
            result.putFrameAnalysis(entry.getKey(), analysis);
        }

        // 2. 验证方向（支撑位必须 < 当前价，阻力位必须 > 当前价）
        validateDirection(allSupports, allResistances, currentPrice);
        // 同步清理各时间级别中的无效位
        for (TimeFrameAnalysis analysis : frameAnalyses.values()) {
            analysis.getSupportLevels().removeIf(s -> s.getPrice() >= currentPrice);
            analysis.getResistanceLevels().removeIf(r -> r.getPrice() <= currentPrice);
        }

        // 3. 检测跨级别重叠
        List<Level> overlappedSupports = findOverlaps(allSupports);
        List<Level> overlappedResistances = findOverlaps(allResistances);
        result.getOverlappedSupports().addAll(overlappedSupports);
        result.getOverlappedResistances().addAll(overlappedResistances);

        // 4. 建立三层防御体系（在验证之后）
        buildDefenseLayers(result, currentPrice);

        return result;
    }

    /**
     * 验证方向：支撑位必须 < 当前价，阻力位必须 > 当前价
     */
    private static void validateDirection(List<Level> supports, List<Level> resistances, double currentPrice) {
        supports.removeIf(s -> s.getPrice() >= currentPrice);
        resistances.removeIf(r -> r.getPrice() <= currentPrice);
    }

    /**
     * 检测跨级别重叠的支撑/阻力位
     * 核心要求: 重叠必须来自不同的时间级别（同一级别内相近价位不算"多级别重叠"）
     *
     * @param levels 所有支撑/阻力位
     * @return 重叠位列表
     */
    private static List<Level> findOverlaps(List<Level> levels) {
        List<Level> overlapped = new ArrayList<>();
        if (levels.isEmpty()) return overlapped;

        // 按价格分组: 价格相近的归为一组
        List<List<Level>> groups = groupByProximity(levels);

        for (List<Level> group : groups) {
            if (group.size() >= 2) {
                // 检查是否跨级别：统计组内不同时间级别数
                Set<TimeFrame> timeFrames = new HashSet<>();
                for (Level l : group) {
                    timeFrames.add(l.getTimeFrame());
                }
                // 只有来自不同时间级别的才算是"多级别重叠"
                if (timeFrames.size() >= 2) {
                    Level merged = mergeGroup(group);
                    overlapped.add(merged);
                }
            }
        }

        return overlapped;
    }

    /**
     * 按价格相近程度分组
     * 使用简单的贪心聚类: 价格从低到高排序，依次将间距 <= tolerance 的归入同一组
     */
    private static List<List<Level>> groupByProximity(List<Level> levels) {
        List<Level> sorted = new ArrayList<>(levels);
        sorted.sort(Comparator.comparingDouble(Level::getPrice));

        List<List<Level>> groups = new ArrayList<>();
        List<Level> currentGroup = new ArrayList<>();
        currentGroup.add(sorted.get(0));

        for (int i = 1; i < sorted.size(); i++) {
            Level prev = currentGroup.get(currentGroup.size() - 1);
            Level curr = sorted.get(i);

            double gapPercent = (curr.getPrice() - prev.getPrice()) / prev.getPrice() * 100;
            if (gapPercent <= OVERLAP_TOLERANCE_PERCENT) {
                currentGroup.add(curr);
            } else {
                groups.add(currentGroup);
                currentGroup = new ArrayList<>();
                currentGroup.add(curr);
            }
        }
        groups.add(currentGroup);

        return groups;
    }

    /**
     * 合并一组相近的Level
     * 取加权平均价，重叠数 = 不同时间级别数
     */
    private static Level mergeGroup(List<Level> group) {
        // 取平均价格
        double avgPrice = group.stream().mapToDouble(Level::getPrice).average().orElse(0);

        // 统计不同时间级别数
        Set<TimeFrame> timeFrames = new HashSet<>();
        for (Level l : group) {
            timeFrames.add(l.getTimeFrame());
        }
        int overlapCount = timeFrames.size();

        // 用强度最高的Level作为模板（保留来源信息）
        Level template = group.stream()
                .max(Comparator.comparingInt(Level::getStrength))
                .orElse(group.get(0));

        Level merged = new Level(avgPrice, template.getType(), template.getSource(), template.getTimeFrame());
        merged.setOverlapCount(overlapCount);

        // 计算测试次数（取最大值）
        int maxTestCount = group.stream().mapToInt(Level::getTestCount).max().orElse(0);
        merged.setTestCount(maxTestCount);

        // 根据重叠数设定强度
        StrengthLevel strength;
        if (overlapCount >= 3) {
            strength = StrengthLevel.VERY_STRONG;
        } else if (overlapCount >= 2) {
            strength = StrengthLevel.STRONG;
        } else {
            strength = StrengthLevel.MEDIUM;
        }

        // 有测试验证则提升（最多到极强）
        if (maxTestCount >= 2 && strength.getValue() < StrengthLevel.VERY_STRONG.getValue()) {
            strength = StrengthLevel.fromValue(strength.getValue() + 1);
        }

        merged.setStrength(strength.getValue());
        merged.setStrengthLabel(strength.getLabel() + " " + strength.getStars());

        return merged;
    }

    /**
     * 建立三层防御体系
     * 短期: 4H级别离当前价最近的有效支撑/阻力
     * 中期: 1D级别离当前价最近的有效支撑/阻力
     * 长期: 1W级别离当前价最近的有效支撑/阻力
     * 优先使用多级别重叠位（如果有重叠位属于该时间级别，优先选用）
     */
    private static void buildDefenseLayers(AnalysisResult result, double currentPrice) {
        // 支撑位三层防御
        buildDefenseForType(result, LevelType.SUPPORT, currentPrice);
        // 阻力位三层防御
        buildDefenseForType(result, LevelType.RESISTANCE, currentPrice);
    }

    private static void buildDefenseForType(AnalysisResult result, LevelType type, double currentPrice) {
        Map<String, Level> layers = (type == LevelType.SUPPORT)
                ? result.getDefenseSupportLayers()
                : result.getDefenseResistanceLayers();

        // 获取多级别重叠位
        List<Level> overlapped = (type == LevelType.SUPPORT)
                ? result.getOverlappedSupports()
                : result.getOverlappedResistances();

        // 收集所有可用位（重叠 + 单级别），确保每层选不同价位
        List<Level> allCandidates = new ArrayList<>();
        allCandidates.addAll(overlapped);
        for (TimeFrameAnalysis analysis : result.getFrameAnalyses().values()) {
            List<Level> levels = (type == LevelType.SUPPORT)
                    ? analysis.getSupportLevels()
                    : analysis.getResistanceLevels();
            allCandidates.addAll(levels);
        }

        // 按离当前价距离升序排列（最近优先）
        allCandidates.sort(Comparator.comparingDouble(l -> Math.abs(l.distancePercent(currentPrice))));

        // 确保相邻层间距 > 1%，避免重复
        List<Level> selectedLayers = new ArrayList<>();
        for (Level candidate : allCandidates) {
            if (selectedLayers.isEmpty()) {
                selectedLayers.add(candidate);
            } else {
                boolean tooClose = false;
                for (Level existing : selectedLayers) {
                    double gap = Math.abs(candidate.getPrice() - existing.getPrice()) / existing.getPrice() * 100;
                    if (gap <= MERGE_MIN_GAP_PERCENT) {
                        tooClose = true;
                        break;
                    }
                }
                if (!tooClose) {
                    selectedLayers.add(candidate);
                }
            }
            if (selectedLayers.size() >= 3) break;
        }

        // 分配到三层: 按价格距离排序，最近→短期，次近→中期，最远→长期
        if (selectedLayers.size() >= 1) layers.put("短期", selectedLayers.get(0));
        if (selectedLayers.size() >= 2) layers.put("中期", selectedLayers.get(1));
        if (selectedLayers.size() >= 3) layers.put("长期", selectedLayers.get(2));
    }

    /**
     * 合并相近的支撑/阻力位
     * 规则: 间距 <= MERGE_MIN_GAP_PERCENT 的相邻位，保留强度更高的那个
     *
     * @param levels 支撑/阻力位列表（已按价格排序）
     * @return 合并后的列表
     */
    public static List<Level> mergeCloseLevels(List<Level> levels) {
        if (levels.size() <= 1) return new ArrayList<>(levels);

        List<Level> sorted = new ArrayList<>(levels);
        sorted.sort(Comparator.comparingDouble(Level::getPrice));

        List<Level> merged = new ArrayList<>();
        Level current = sorted.get(0);

        for (int i = 1; i < sorted.size(); i++) {
            Level next = sorted.get(i);
            double gapPercent = (next.getPrice() - current.getPrice()) / current.getPrice() * 100;

            if (gapPercent <= MERGE_MIN_GAP_PERCENT) {
                // 间距太近，保留强度更高的
                if (next.getStrength() > current.getStrength()) {
                    current = next;
                }
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);

        return merged;
    }
}
