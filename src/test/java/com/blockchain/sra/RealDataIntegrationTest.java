package com.blockchain.sra;


import com.blockchain.sra.fetcher.BinanceKlineFetcher;
import com.blockchain.sra.fetcher.OkxKlineFetcher;
import com.blockchain.sra.model.AnalysisResult;
import com.blockchain.sra.model.Level;
import com.blockchain.sra.model.TimeFrameAnalysis;
import com.blockchain.sra.report.ReportGenerator;

import java.io.File;
import java.io.IOException;

/**
 * 真实数据集成测试
 * <p>
 * 从交易所(Binance/OKX)拉取真实K线数据 → 生成CSV → 执行支撑阻力位分析 → 输出Markdown报告
 * <p>
 * 使用方式:
 * <pre>
 *   # 默认: 分析BTCUSDT
 *   java -cp support-resistance-analyzer-1.0.0.jar com.canpoint.sra.RealDataIntegrationTest
 *
 *   # 指定交易对
 *   java -cp support-resistance-analyzer-1.0.0.jar com.canpoint.sra.RealDataIntegrationTest ETHUSDT
 *
 *   # 指定交易对和输出目录
 *   java -cp support-resistance-analyzer-1.0.0.jar com.canpoint.sra.RealDataIntegrationTest BTCUSDT /tmp/kline
 * </pre>
 * <p>
 * 数据源优先级: Binance → OKX (自动降级)
 */
public class RealDataIntegrationTest {

    /** 默认交易对 */
    private static final String DEFAULT_SYMBOL = "BTCUSDT";

    /** CSV临时目录 */
    private static final String DEFAULT_OUTPUT_DIR = "/Volumes/husky/workspace/Blockchain/doc/kdata/lines/" + DEFAULT_SYMBOL;

    public static void main(String[] args) {
        String symbol = DEFAULT_SYMBOL;
        String outputDir = DEFAULT_OUTPUT_DIR;

        System.out.println("============================================================");
        System.out.println("  支撑阻力位分析工具 - 真实数据集成测试");
        System.out.println("============================================================");
        System.out.printf("  交易对: %s%n", symbol);
        System.out.printf("  CSV目录: %s%n%n", outputDir);

        try {
            // ========== Step 1: 从交易所拉取K线数据（合并到单个CSV文件）==========
            System.out.println("[Step 1] 从交易所拉取K线数据（合并到单个CSV文件）...");
            String mergedCsvPath = fetchMergedKlineData(symbol, outputDir);
            System.out.printf("  合并CSV: %s%n%n", mergedCsvPath);

            // ========== Step 2: 执行分析（使用合并的CSV文件）==========
            System.out.println("[Step 2] 执行多时间级别支撑阻力位分析...");
            AnalysisResult result = SupportResistanceAnalyzer.analyzeFromMerged(symbol, mergedCsvPath);
            System.out.println();

            // ========== Step 3: 输出控制台报告 ==========
            System.out.println("[Step 3] 生成分析报告...");
            String consoleReport = ReportGenerator.generateConsoleReport(result);
            System.out.println(consoleReport);

            // ========== Step 4: 输出Markdown报告文件 ==========
            String reportPath = outputDir + File.separator + symbol + "_支撑阻力分析报告.md";
            ReportGenerator.generateMarkdownReport(result, reportPath);
            System.out.printf("Markdown报告已保存至: %s%n%n", reportPath);

            // ========== Step 5: 验证结果 ==========
            System.out.println("[Step 4] 结果验证...");
            validateResult(result);

            System.out.println();
            System.out.println("============================================================");
            System.out.println("  真实数据集成测试完成！");
            System.out.println("============================================================");

        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * 从交易所拉取K线数据（Binance优先，OKX降级）
     */
    private static String[] fetchKlineData(String symbol, String outputDir) throws IOException {
        // 尝试Binance
        try {
            System.out.println("  -> 尝试Binance API...");
            BinanceKlineFetcher fetcher = new BinanceKlineFetcher();
            fetcher.setMaxRetries(2);
            return fetcher.fetchMultiTimeFrameCsv(symbol, outputDir);
        } catch (IOException e) {
            System.err.println("  x Binance不可用: " + e.getMessage());
        }

        // 降级到OKX
        System.out.println("  -> 降级到OKX API...");
        try {
            OkxKlineFetcher fetcher = new OkxKlineFetcher();
            fetcher.setMaxRetries(2);
            return fetcher.fetchMultiTimeFrameCsv(symbol, outputDir);
        } catch (IOException e) {
            System.err.println("  x OKX也不可用: " + e.getMessage());
        }

        throw new IOException("所有交易所API均不可用，请检查网络连接");
    }

    /**
     * 从交易所拉取K线数据并合并到单个CSV文件（Binance优先，OKX降级）
     */
    private static String fetchMergedKlineData(String symbol, String outputDir) throws IOException {
        // 尝试Binance
        try {
            System.out.println("  -> 尝试Binance API...");
            BinanceKlineFetcher fetcher = new BinanceKlineFetcher();
            fetcher.setMaxRetries(2);
            return fetcher.fetchMultiTimeFrameToSingleCsv(symbol, outputDir);
        } catch (IOException e) {
            System.err.println("  x Binance不可用: " + e.getMessage());
        }

        // 降级到OKX
        System.out.println("  -> 降级到OKX API...");
        try {
            OkxKlineFetcher fetcher = new OkxKlineFetcher();
            fetcher.setMaxRetries(2);
            return fetcher.fetchMultiTimeFrameToSingleCsv(symbol, outputDir);
        } catch (IOException e) {
            System.err.println("  x OKX也不可用: " + e.getMessage());
        }

        throw new IOException("所有交易所API均不可用，请检查网络连接");
    }

    /**
     * 验证分析结果的合理性
     */
    private static void validateResult(AnalysisResult result) {
        int issues = 0;

        // 检查1: 各级别是否都有分析结果
        if (result.getFrameAnalyses().size() < 3) {
            System.err.println("  x 警告: 时间级别分析不完整，仅 " + result.getFrameAnalyses().size() + " 个级别");
            issues++;
        } else {
            System.out.println("  v 三级时间框架分析完整");
        }

        // 检查2: 支撑位应低于当前价，阻力位应高于当前价
        double currentPrice = result.getCurrentPrice();
        boolean directionOk = true;
        for (TimeFrameAnalysis analysis : result.getFrameAnalyses().values()) {
            for (Level s : analysis.getSupportLevels()) {
                if (s.getPrice() > currentPrice) {
                    System.err.printf("  x 警告: [%s] 支撑位 %.2f 高于当前价 %.2f%n",
                            s.getTimeFrame().getCode(), s.getPrice(), currentPrice);
                    directionOk = false;
                    issues++;
                }
            }
            for (Level r : analysis.getResistanceLevels()) {
                if (r.getPrice() < currentPrice) {
                    System.err.printf("  x 警告: [%s] 阻力位 %.2f 低于当前价 %.2f%n",
                            r.getTimeFrame().getCode(), r.getPrice(), currentPrice);
                    directionOk = false;
                    issues++;
                }
            }
        }
        if (directionOk) {
            System.out.println("  v 支撑/阻力位方向验证通过");
        }

        // 检查3: 三层防御体系是否完整
        boolean defenseOk = true;
        if (result.getDefenseSupportLayers().isEmpty()) {
            System.err.println("  x 警告: 三层支撑防线为空");
            defenseOk = false;
        }
        if (result.getDefenseResistanceLayers().isEmpty()) {
            System.err.println("  x 警告: 三层阻力防线为空");
            defenseOk = false;
        }
        if (defenseOk) {
            System.out.println("  v 三层防御体系完整");
        }

        // 检查4: 三层防御方向验证
        boolean defenseDirectionOk = true;
        for (Level s : result.getDefenseSupportLayers().values()) {
            if (s.getPrice() > currentPrice) {
                System.err.printf("  x 警告: 支撑防线 %.2f 高于当前价 %.2f%n", s.getPrice(), currentPrice);
                defenseDirectionOk = false;
                issues++;
            }
        }
        for (Level r : result.getDefenseResistanceLayers().values()) {
            if (r.getPrice() < currentPrice) {
                System.err.printf("  x 警告: 阻力防线 %.2f 低于当前价 %.2f%n", r.getPrice(), currentPrice);
                defenseDirectionOk = false;
                issues++;
            }
        }
        if (defenseDirectionOk) {
            System.out.println("  v 三层防御方向验证通过");
        }

        // 检查5: 多级别重叠位检测
        int overlapCount = result.getOverlappedSupports().size() + result.getOverlappedResistances().size();
        System.out.printf("  v 发现 %d 个多级别重叠位 (支撑%d + 阻力%d)%n",
                overlapCount, result.getOverlappedSupports().size(), result.getOverlappedResistances().size());

        // 检查6: 各级别是否都识别到了关键位
        for (TimeFrameAnalysis analysis : result.getFrameAnalyses().values()) {
            if (analysis.getSupportLevels().isEmpty() && analysis.getResistanceLevels().isEmpty()) {
                System.err.printf("  x 警告: [%s] 未识别到任何支撑/阻力位%n", analysis.getTrend());
                issues++;
            }
        }

        if (issues > 0) {
            System.err.printf("  ! 共发现 %d 个异常，请检查%n", issues);
        } else {
            System.out.println("  v 全部验证通过，无异常");
        }
    }
}
