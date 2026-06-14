package com.blockchain.sra.core;


import com.blockchain.sra.model.*;

import java.util.List;

/**
 * 支撑/阻力位强度判断器
 *
 * 支撑位强度标准:
 *   极强(5星): 多级别重叠 + 2次以上测试验证
 *   强(4星):   单级别最近显著低点 + 1次测试
 *   中(3星):   斐波那契38.2%~61.8%回调位
 *   弱(2星):   斐波那契23.6%、78.6%回调位
 *   极弱(1星): 历史局部低点（未测试）
 *
 * 阻力位强度标准（对称）:
 *   极强(5星): 多级别重叠 + 2次以上测试验证
 *   强(4星):   单级别最近显著高点 + 1次测试
 *   中(3星):   斐波那契127.2%~161.8%扩展
 *   弱(2星):   斐波那契100%、200%、261.8%扩展
 *   极弱(1星): 历史局部高点（未测试）
 */
public class StrengthEvaluator {

    /** 相近价位的容差百分比（用于测试次数统计） */
    private static final double TEST_TOLERANCE = 0.005; // 0.5%

    /**
     * 评估支撑位强度
     *
     * @param level       支撑位
     * @param klines      K线数据（该时间级别）
     * @param overlapCount 跨级别重叠数
     * @return 更新后的强度等级
     */
    public static StrengthLevel evaluateSupportStrength(Level level, List<Kline> klines, int overlapCount) {
        int testCount = LocalExtremumFinder.countTests(klines, level.getPrice(), LevelType.SUPPORT, TEST_TOLERANCE);
        level.setTestCount(testCount);
        level.setOverlapCount(overlapCount);

        return evaluateStrength(level, overlapCount, testCount);
    }

    /**
     * 评估阻力位强度
     *
     * @param level       阻力位
     * @param klines      K线数据（该时间级别）
     * @param overlapCount 跨级别重叠数
     * @return 更新后的强度等级
     */
    public static StrengthLevel evaluateResistanceStrength(Level level, List<Kline> klines, int overlapCount) {
        int testCount = LocalExtremumFinder.countTests(klines, level.getPrice(), LevelType.RESISTANCE, TEST_TOLERANCE);
        level.setTestCount(testCount);
        level.setOverlapCount(overlapCount);

        return evaluateStrength(level, overlapCount, testCount);
    }

    /**
     * 通用强度评估逻辑
     * 优先级: 多级别重叠 + 测试次数 > 斐波那契固有强度
     */
    private static StrengthLevel evaluateStrength(Level level, int overlapCount, int testCount) {
        StrengthLevel finalStrength;

        // 规则1: 多级别重叠 + 测试验证 → 极强(5星)
        if (overlapCount >= 3 && testCount >= 2) {
            finalStrength = StrengthLevel.VERY_STRONG;
        }
        // 规则2: 2级别重叠 + 测试验证 → 强(4星)
        else if (overlapCount >= 2 && testCount >= 1) {
            finalStrength = StrengthLevel.STRONG;
        }
        // 规则3: 最近显著高低点 + 测试 → 强(4星)
        else if (isRecentSignificant(level) && testCount >= 1) {
            finalStrength = StrengthLevel.STRONG;
        }
        // 规则4: 使用斐波那契固有强度
        else {
            StrengthLevel inherent = StrengthLevel.fromValue(level.getStrength());

            // 如果有测试验证但无重叠，提升1级（不超过4星）
            if (testCount >= 2 && inherent.getValue() < 4) {
                finalStrength = StrengthLevel.fromValue(inherent.getValue() + 1);
            } else {
                finalStrength = inherent;
            }
        }

        level.setStrength(finalStrength.getValue());
        level.setStrengthLabel(finalStrength.getLabel() + " " + finalStrength.getStars());

        return finalStrength;
    }

    /**
     * 判断是否为"最近显著高低点"
     */
    private static boolean isRecentSignificant(Level level) {
        return level.getSource() == LevelSource.RECENT_SIGNIFICANT_LOW
                || level.getSource() == LevelSource.RECENT_SIGNIFICANT_HIGH;
    }
}
