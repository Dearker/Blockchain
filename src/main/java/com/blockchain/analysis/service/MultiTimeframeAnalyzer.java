package com.blockchain.analysis.service;

import com.blockchain.analysis.model.AnalysisResult;
import com.blockchain.analysis.model.PriceLevel;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 多级别对比分析工具
 * 对应Python版 find_overlapping_levels() 和 classify_defense_layers()
 */
@Slf4j
//@Service
public class MultiTimeframeAnalyzer {

    /**
     * 寻找多级别重叠价位
     * @param allLevels 所有级别的价格位列表（需包含 timeframe 字段）
     * @param threshold  价格差异阈值（默认 2%）
     * @return 重叠价位列表
     */
    public List<com.blockchain.analysis.model.OverlappingLevel> findOverlappingLevels(
            List<PriceLevel> allLevels, double threshold) {

        List<com.blockchain.analysis.model.OverlappingLevel> overlapping = new ArrayList<>();
        allLevels.sort(Comparator.comparing(PriceLevel::getPrice));

        for (int i = 0; i < allLevels.size(); i++) {
            List<PriceLevel> matching = new ArrayList<>();
            matching.add(allLevels.get(i));

            for (int j = 0; j < allLevels.size(); j++) {
                if (i == j) continue;
                double priceDiff = Math.abs(allLevels.get(j).getPrice() - allLevels.get(i).getPrice())
                        / allLevels.get(i).getPrice();
                if (priceDiff < threshold) {
                    matching.add(allLevels.get(j));
                }
            }

            Set<String> tfs = new HashSet<>();
            for (PriceLevel m : matching) {
                if (m.getTimeframe() != null) tfs.add(m.getTimeframe());
            }

            if (tfs.size() >= 2) {
                com.blockchain.analysis.model.OverlappingLevel ol
                        = new com.blockchain.analysis.model.OverlappingLevel();
                double avgPrice = matching.stream()
                        .mapToDouble(PriceLevel::getPrice).average().orElse(0.0);
                ol.setPrice(Math.round(avgPrice * 100.0) / 100.0);
                ol.setTimeframes(new ArrayList<>(tfs));
                ol.setTypes(matching.stream()
                        .map(PriceLevel::getType).distinct().toList());
                ol.setOverlapCount(matching.size());
                ol.setStrength(tfs.size() >= 3
                        ? "★★★★★ 极强（三级别重叠）"
                        : "★★★☆☆ 强（两级别重叠）");
                overlapping.add(ol);
            }
        }

        // 去重（按价格四舍五入2位）
        Set<Double> seen = new HashSet<>();
        List<com.blockchain.analysis.model.OverlappingLevel> unique = new ArrayList<>();
        for (com.blockchain.analysis.model.OverlappingLevel o : overlapping) {
            double key = Math.round(o.getPrice() * 100.0) / 100.0;
            if (!seen.contains(key)) {
                seen.add(key);
                unique.add(o);
            }
        }
        return unique;
    }

    /**
     * 分类三层防御体系
     * Layer1: 距离 ±1-3%
     * Layer2: 距离 ±3-10%
     * Layer3: 距离 ±10%以上
     */
    public Map<String, com.blockchain.analysis.model.DefenseLayer> classifyDefenseLayers(
            List<PriceLevel> supports, List<PriceLevel> resistances) {

        Map<String, com.blockchain.analysis.model.DefenseLayer> result = new LinkedHashMap<>();

        // Layer 1: ±1-3%
        com.blockchain.analysis.model.DefenseLayer layer1
                = new com.blockchain.analysis.model.DefenseLayer();
        layer1.setSupports(supports.stream()
                .filter(s -> s.getDistance() <= 3).limit(3).toList());
        layer1.setResistances(resistances.stream()
                .filter(r -> r.getDistance() <= 3).limit(3).toList());
        result.put("layer1", layer1);

        // Layer 2: ±3-10%
        com.blockchain.analysis.model.DefenseLayer layer2
                = new com.blockchain.analysis.model.DefenseLayer();
        layer2.setSupports(supports.stream()
                .filter(s -> s.getDistance() > 3 && s.getDistance() <= 10).limit(3).toList());
        layer2.setResistances(resistances.stream()
                .filter(r -> r.getDistance() > 3 && r.getDistance() <= 10).limit(3).toList());
        result.put("layer2", layer2);

        // Layer 3: ±10%以上
        com.blockchain.analysis.model.DefenseLayer layer3
                = new com.blockchain.analysis.model.DefenseLayer();
        layer3.setSupports(supports.stream()
                .filter(s -> s.getDistance() > 10).limit(2).toList());
        layer3.setResistances(resistances.stream()
                .filter(r -> r.getDistance() > 10).limit(2).toList());
        result.put("layer3", layer3);

        return result;
    }

    /**
     * 从多级别分析结果中汇总所有支撑位和阻力位
     */
    public void collectAllLevels(Map<String, AnalysisResult> results,
                                  List<PriceLevel> allSupports,
                                  List<PriceLevel> allResistances) {
        for (Map.Entry<String, AnalysisResult> entry : results.entrySet()) {
            String tf = entry.getKey();
            AnalysisResult r = entry.getValue();
            for (PriceLevel s : r.getSupports()) {
                s.setTimeframe(tf);
                allSupports.add(s);
            }
            for (PriceLevel rs : r.getResistances()) {
                rs.setTimeframe(tf);
                allResistances.add(rs);
            }
        }
    }
}
