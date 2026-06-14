package com.blockchain.analysis;

import com.blockchain.analysis.model.AnalysisResult;
import com.blockchain.analysis.model.DefenseLayer;
import com.blockchain.analysis.model.KlineData;
import com.blockchain.analysis.model.PriceLevel;
import com.blockchain.analysis.service.KlineDataManager;
import com.blockchain.analysis.service.MultiTimeframeAnalyzer;
import com.blockchain.analysis.service.ReportGenerator;
import com.blockchain.analysis.service.SupportResistanceAnalyzer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多时间级别支撑阻力位分析 — 主程序入口
 * 对应Python版 main() 函数
 *
 * 运行方式：
 *   mvn spring-boot:run -Dspring-boot.run.arguments="--symbol=BTCUSDT"
 *   或在 IDE 中直接运行 main() 方法
 */
@Slf4j
//@Component
public class SupportResistanceApp implements CommandLineRunner {

    /** 报告输出目录（与Python版一致） */
    private static final String REPORT_DIR = "D:/kdata/reports";

    /** 时间级别配置（与Python版 TIMEFRAMES 一致） */
    private static final Map<String, TfConfig> TIMEFRAMES = Map.of(
            "4h", new TfConfig(300, 3),
            "1d", new TfConfig(180, 5),
            "1w", new TfConfig(100, 5)
    );

    @Autowired
    private KlineDataManager klineDataManager;

    @Autowired
    private ReportGenerator reportGenerator;

    @Autowired
    private MultiTimeframeAnalyzer multiTimeframeAnalyzer;

    public static void main(String[] args) {
        // 直接运行此类时，通过 Spring Boot 启动
        SpringApplication.run(SupportResistanceApp.class, args);
    }

    @Override
    public void run(String... args) {
        // 解析命令行参数
        String symbol = "BTCUSDT";
        String reportDir = REPORT_DIR;

        for (int i = 0; i < args.length; i++) {
            if ("--symbol".equals(args[i]) && i + 1 < args.length) {
                symbol = args[i + 1].toUpperCase();
            } else if ("--report-dir".equals(args[i]) && i + 1 < args.length) {
                reportDir = args[i + 1];
            }
        }

        runAnalysis(symbol, reportDir);
    }

    public void runAnalysis(String symbol) {
        runAnalysis(symbol, REPORT_DIR);
    }

    public void runAnalysis(String symbol, String reportDir) {
        symbol = symbol.toUpperCase();
        log.info("============================================================");
        log.info("{} 多级别支撑阻力位分析（Java版）", symbol);
        log.info("============================================================");

        // 确保报告目录存在
        try {
            Files.createDirectories(Paths.get(reportDir));
        } catch (IOException e) {
            log.error("创建报告目录失败: {}", reportDir, e);
            return;
        }

        KlineDataManager dm = new KlineDataManager();
        Map<String, AnalysisResult> results = new LinkedHashMap<>();

        // 逐时间级别分析
        for (Map.Entry<String, TfConfig> entry : TIMEFRAMES.entrySet()) {
            String tf = entry.getKey();
            TfConfig cfg = entry.getValue();
            log.info("\n[{}] 更新数据...", tf.toUpperCase());

            List<KlineData> data = dm.updateData(symbol, tf, cfg.getLimit());
            if (data == null || data.isEmpty()) {
                log.warn("  ✗ 无数据，跳过");
                continue;
            }
            if (data.size() < cfg.getLimit()) {
                log.warn("  ⚠️ 数据不足（{} < {}），仍分析", data.size(), cfg.getLimit());
            }

            log.info("[{}] 分析支撑阻力位...", tf.toUpperCase());
            SupportResistanceAnalyzer analyzer =
                    new SupportResistanceAnalyzer(symbol, tf, data, cfg.getWindow());
            AnalysisResult result = analyzer.generateAnalysisResult();
            results.put(tf, result);

            log.info("  ✓ 完成：{}支撑 / {}阻力",
                    result.getSupportsCount(), result.getResistancesCount());
            log.info("  当前价: ${}  |  高点: ${}  |  低点: ${}",
                    formatPrice(result.getCurrentPrice()),
                    formatPrice(result.getRecentHigh()),
                    formatPrice(result.getRecentLow()));
        }

        if (results.isEmpty()) {
            log.error("✗ 无数据，无法生成报告");
            return;
        }

        // 生成报告
        String report = reportGenerator.generateReport(symbol, results);

        // 保存报告到文件
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String reportFile = reportDir + "/" + symbol + "_analysis_" + timestamp + ".md";
        try (FileWriter writer = new FileWriter(reportFile)) {
            writer.write(report);
            log.info("\n✓ 报告已生成: {}", reportFile);
        } catch (IOException e) {
            log.error("✗ 报告保存失败", e);
        }

        // 输出关键结论
        printKeyConclusions(symbol, results);
        log.info("\n✓ 完成！数据: D:/kdata/lines\\{}  |  报告: {}", symbol, reportFile);
    }

    /**
     * 输出关键结论到控制台
     */
    private void printKeyConclusions(String symbol, Map<String, AnalysisResult> results) {
        log.info("\n{}", "=" .repeat(60));
        log.info("关键结论");
        log.info("{}", "=" .repeat(60));

        if (!results.containsKey("4h")) return;

        double cp = results.get("4h").getCurrentPrice();
        log.info("当前价格: ${}", formatPrice(cp));

        // 汇总所有级别的价格位
        List<PriceLevel> allSupports = new ArrayList<>();
        List<PriceLevel> allResistances = new ArrayList<>();
        for (AnalysisResult r : results.values()) {
            if (r.getSupports() != null) {
                for (PriceLevel s : r.getSupports()) {
                    s.setTimeframe(r.getTimeframe());
                    allSupports.add(s);
                }
            }
            if (r.getResistances() != null) {
                for (PriceLevel rs : r.getResistances()) {
                    rs.setTimeframe(r.getTimeframe());
                    allResistances.add(rs);
                }
            }
        }

        Map<String, DefenseLayer> dl =
                multiTimeframeAnalyzer.classifyDefenseLayers(allSupports, allResistances);

        printLayer(dl, "layer1", "短期±1-3%");
        printLayer(dl, "layer2", "中期±3-10%");
        printLayer(dl, "layer3", "长期±10%以上");
    }

    private void printLayer(Map<String, DefenseLayer> dl, String key, String label) {
        DefenseLayer layer = dl.get(key);
        if (layer == null) return;
        if (layer.getSupports() != null && !layer.getSupports().isEmpty()) {
            PriceLevel s = layer.getSupports().get(0);
            log.info("  支撑({}): ${} (-{}%)",
                    label, formatPrice(s.getPrice()), formatPct(s.getDistance()));
        }
        if (layer.getResistances() != null && !layer.getResistances().isEmpty()) {
            PriceLevel r = layer.getResistances().get(0);
            log.info("  阻力({}): ${} (+{}%)",
                    label, formatPrice(r.getPrice()), formatPct(r.getDistance()));
        }
    }

    // ==================== 工具方法 ====================

    private String formatPrice(double price) {
        return String.format("%,.2f", price);
    }

    private String formatPct(double pct) {
        return String.format("%.2f", pct);
    }

    /**
     * 时间级别配置（不可变记录类）
     */
    public static class TfConfig {

        /**
         * K线数量要求
         */
        private final int limit;

        /**
         * 局部高低点窗口大小
         */
        private final int window;

        public TfConfig(int limit, int window) {
            this.limit = limit;
            this.window = window;
        }

        public int getLimit() { return limit; }
        public int getWindow() { return window; }
    }
}
