package com.blockchain.sra;


import com.blockchain.sra.core.MultiTimeFrameAnalyzer;
import com.blockchain.sra.core.SingleTimeFrameAnalyzer;
import com.blockchain.sra.model.AnalysisResult;
import com.blockchain.sra.model.Kline;
import com.blockchain.sra.model.TimeFrame;
import com.blockchain.sra.model.TimeFrameAnalysis;
import com.blockchain.sra.report.ReportGenerator;
import com.blockchain.sra.util.CsvKlineReader;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多时间级别支撑阻力位分析工具 - 主入口
 *
 * 使用方式:
 *   java -jar support-resistance-analyzer.jar <交易对> <4H CSV> <1D CSV> <1W CSV> [输出文件]
 *
 * CSV格式要求: date,open,high,low,close,volume
 * 支持多种日期格式，自动检测
 *
 * 示例:
 *   java -jar sra.jar BTCUSDT btc_4h.csv btc_1d.csv btc_1w.csv
 *   java -jar sra.jar BTCUSDT btc_4h.csv btc_1d.csv btc_1w.csv report.md
 */
public class SupportResistanceAnalyzer {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }

        String symbol = args[0];
        String csvPath = args[1];  // 现在只需要一个CSV文件路径（可以是合并的或独立的）
        String outputFile = args.length >= 3 ? args[2] : null;

        try {
            AnalysisResult result;

            // 智能检测CSV格式
            if (isMergedCsv(csvPath)) {
                System.out.println("检测到合并的CSV文件，使用合并模式分析...");
                result = analyzeFromMerged(symbol, csvPath);
            } else {
                // 如果不是合并文件，则需要三个独立的CSV文件
                if (args.length < 4) {
                    System.err.println("错误: 使用独立CSV文件需要提供4H、1D、1W三个文件");
                    printUsage();
                    System.exit(1);
                }
                String csv4h = args[1];
                String csv1d = args[2];
                String csv1w = args[3];
                outputFile = args.length >= 5 ? args[4] : null;
                System.out.println("检测到独立CSV文件，使用独立模式分析...");
                result = analyze(symbol, csv4h, csv1d, csv1w);
            }

            // 控制台输出
            String consoleReport = ReportGenerator.generateConsoleReport(result);
            System.out.println(consoleReport);

            // 文件输出
            if (outputFile != null) {
                ReportGenerator.generateMarkdownReport(result, outputFile);
                System.out.println("报告已保存至: " + outputFile);
            }

        } catch (Exception e) {
            System.err.println("分析失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * 检测CSV文件是否为合并格式（包含timeframe列）
     */
    private static boolean isMergedCsv(String csvPath) {
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(csvPath))) {
            String header = reader.readLine();
            if (header != null) {
                String[] columns = header.split(",");
                for (String col : columns) {
                    if ("timeframe".equalsIgnoreCase(col.trim())) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // 如果无法读取，假设不是合并文件
        }
        return false;
    }

    /**
     * 执行完整分析流程
     *
     * @param symbol 交易对名称
     * @param csv4h  4H K线CSV文件路径
     * @param csv1d  1D K线CSV文件路径
     * @param csv1w  1W K线CSV文件路径
     * @return 综合分析结果
     */
    public static AnalysisResult analyze(String symbol, String csv4h, String csv1d, String csv1w) throws IOException {
        // Step 1: 读取CSV数据
        System.out.println("正在读取K线数据...");
        List<Kline> klines4h = CsvKlineReader.readWithLookback(csv4h, TimeFrame.H4.getLookback());
        List<Kline> klines1d = CsvKlineReader.readWithLookback(csv1d, TimeFrame.D1.getLookback());
        List<Kline> klines1w = CsvKlineReader.readWithLookback(csv1w, TimeFrame.W1.getLookback());

        System.out.printf("  4H: %d 根, 1D: %d 根, 1W: %d 根%n", klines4h.size(), klines1d.size(), klines1w.size());

        return analyze(symbol, klines4h, klines1d, klines1w);
    }

    /**
     * 执行完整分析流程（使用合并的CSV文件）
     *
     * @param symbol        交易对名称
     * @param mergedCsvPath 合并的CSV文件路径（包含timeframe列）
     * @return 综合分析结果
     */
    public static AnalysisResult analyzeFromMerged(String symbol, String mergedCsvPath) throws IOException {
        // Step1: 从合并的CSV文件读取各时间级别数据
        System.out.println("正在从合并CSV文件读取K线数据...");
        List<Kline> klines4h = CsvKlineReader.readFromMergedWithLookback(mergedCsvPath, "4h", TimeFrame.H4.getLookback());
        List<Kline> klines1d = CsvKlineReader.readFromMergedWithLookback(mergedCsvPath, "1d", TimeFrame.D1.getLookback());
        List<Kline> klines1w = CsvKlineReader.readFromMergedWithLookback(mergedCsvPath, "1w", TimeFrame.W1.getLookback());

        System.out.printf("  4H: %d 根, 1D: %d 根, 1W: %d 根%n", klines4h.size(), klines1d.size(), klines1w.size());

        return analyze(symbol, klines4h, klines1d, klines1w);
    }

    /**
     * 执行完整分析流程（传入已加载的K线数据）
     *
     * @param symbol  交易对名称
     * @param klines4h 4H K线数据
     * @param klines1d 1D K线数据
     * @param klines1w 1W K线数据
     * @return 综合分析结果
     */
    public static AnalysisResult analyze(String symbol, List<Kline> klines4h, List<Kline> klines1d, List<Kline> klines1w) {
        // 当前价格取最新的4H收盘价（统一所有级别使用同一价格）
        double currentPrice = klines4h.get(klines4h.size() - 1).getClose();

        // Step 2: 各级别独立分析（传入统一当前价格）
        System.out.println("正在执行单级别分析...");
        TimeFrameAnalysis analysis4h = SingleTimeFrameAnalyzer.analyze(klines4h, TimeFrame.H4, currentPrice);
        TimeFrameAnalysis analysis1d = SingleTimeFrameAnalyzer.analyze(klines1d, TimeFrame.D1, currentPrice);
        TimeFrameAnalysis analysis1w = SingleTimeFrameAnalyzer.analyze(klines1w, TimeFrame.W1, currentPrice);

        // Step 3: 多级别对比（内含方向验证 + 重叠检测 + 三层防御体系）
        System.out.println("正在执行多级别对比分析...");
        Map<TimeFrame, TimeFrameAnalysis> frameAnalyses = new LinkedHashMap<>();
        frameAnalyses.put(TimeFrame.H4, analysis4h);
        frameAnalyses.put(TimeFrame.D1, analysis1d);
        frameAnalyses.put(TimeFrame.W1, analysis1w);

        AnalysisResult result = MultiTimeFrameAnalyzer.analyze(frameAnalyses, currentPrice);
        result.setSymbol(symbol);
        result.setCurrentPrice(currentPrice);
        result.setAnalysisTime(LocalDateTime.now().format(FMT));

        System.out.println("分析完成!");
        return result;
    }

    private static void printUsage() {
        System.out.println("多时间级别支撑阻力位分析工具 v1.0");
        System.out.println();
        System.out.println("用法 1 (推荐): java -jar sra.jar <交易对> <合并CSV> [输出文件]");
        System.out.println("用法 2 (兼容): java -jar sra.jar <交易对> <4H CSV> <1D CSV> <1W CSV> [输出文件]");
        System.out.println();
        System.out.println("参数说明:");
        System.out.println("  交易对     交易对名称，如 BTCUSDT, ETHUSDT");
        System.out.println("  合并CSV    合并的CSV文件路径（包含timeframe列）");
        System.out.println("  4H CSV     4小时线CSV文件路径（独立模式）");
        System.out.println("  1D CSV     日线CSV文件路径（独立模式）");
        System.out.println("  1W CSV     周线CSV文件路径（独立模式）");
        System.out.println("  输出文件   可选，Markdown报告输出路径");
        System.out.println();
        System.out.println("CSV格式:");
        System.out.println("  合并格式: timeframe,date,open,high,low,close,volume");
        System.out.println("  独立格式: date,open,high,low,close,volume");
        System.out.println("日期支持: yyyy-MM-dd / yyyy-MM-dd HH:mm:ss / yyyy/MM/dd / 时间戳");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  合并模式: java -jar sra.jar BTCUSDT btc_merged.csv report.md");
        System.out.println("  独立模式: java -jar sra.jar BTCUSDT btc_4h.csv btc_1d.csv btc_1w.csv report.md");
    }
}
