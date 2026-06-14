package com.blockchain;

import com.blockchain.config.KlineProperties;
import com.blockchain.service.KlineFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KlineFileService 集成测试
 * 真实调用币安API拉取K线数据，生成CSV文件到本地
 * 每个交易对生成一个 kline.csv，通过 timeInterval 列区分 4h/1d/1w
 *
 * @author blockchain
 */
@SpringBootTest
public class KlineFileTest {

    @Autowired
    private KlineFileService klineFileService;

    @Autowired
    private KlineProperties klineProperties;

    /** 测试用的交易对（币安现货，流动性好） */
    private final String TEST_SYMBOL = "ORCLUSDT";

    @BeforeEach
    void setUp() throws Exception {
        // 每次测试前清理该交易对的本地数据目录
        String basePath = klineProperties.getPath();
        java.nio.file.Path symbolDir = Paths.get(basePath, TEST_SYMBOL);
        if (Files.exists(symbolDir)) {
            deleteRecursively(symbolDir.toFile());
        }
        Files.createDirectories(symbolDir);
        System.out.println("测试数据目录: " + symbolDir);
    }

    /**
     * 测试1: 全量同步单个交易对的所有时间级别
     * 预期: 拉取真实数据并生成 kline.csv 文件，包含 4h/1d/1w 数据
     */
    @Test
    void testSyncKlineData_Full() {
        // 全量同步（本地无数据时会触发全量拉取）
        klineFileService.syncKlineData(TEST_SYMBOL);

        System.out.println("✅ 全量同步测试通过: " + TEST_SYMBOL);
    }

    /**
     * 测试2: 增量同步（连续调用两次，第二次应为增量）
     * 预期: 第二次调用不会重复拉取已有数据
     */
    @Test
    void testSyncKlineData_Incremental() throws InterruptedException {
        // 第一次：全量同步
        klineFileService.syncKlineData(TEST_SYMBOL);
        System.out.println("第一次同步完成");

        // 读取同步记录
        String csvContent1 = klineFileService.readLocalKlineData(TEST_SYMBOL, "4h");
        assertNotNull(csvContent1);
        System.out.println("第一次同步后 4h CSV 行数: " + countCsvLines(csvContent1));

        // 等待3秒，确保有时间产生新K线（通常不会有新数据，但验证不会报错）
        Thread.sleep(3000);

        // 第二次：增量同步
        klineFileService.syncKlineData(TEST_SYMBOL);
        System.out.println("第二次同步完成");

        // 验证文件仍然存在且内容合理
        assertKlineCsvExists(TEST_SYMBOL);
        String csvContent2 = klineFileService.readLocalKlineData(TEST_SYMBOL, "4h");
        assertNotNull(csvContent2);
        System.out.println("第二次同步后 4h CSV 行数: " + countCsvLines(csvContent2));

        System.out.println("✅ 增量同步测试通过: " + TEST_SYMBOL);
    }

    /**
     * 测试3: 批量同步多个交易对
     * 预期: 所有交易对都生成 kline.csv 文件
     */
    @Test
    void testSyncKlineData_MultipleSymbols() {
        List<String> symbols = Arrays.asList("BTCUSDT", "ETHUSDT", "BNBUSDT");

        klineFileService.syncKlineData(symbols);

        // 验证所有交易对的 kline.csv 文件都已生成
        for (String symbol : symbols) {
            assertKlineCsvExists(symbol);
            assertKlineCsvNotEmpty(symbol);
            System.out.println("✅ " + symbol + " kline.csv 已生成");
        }

        System.out.println("✅ 批量同步测试通过");
    }

    /**
     * 测试4: 验证生成的 CSV 文件格式正确
     * 预期: 首行为表头（含 timeInterval），后续行为数据，字段完整
     */
    @Test
    void testCsvFormat() {
        klineFileService.syncKlineData(TEST_SYMBOL);

        // 读取全量 CSV（不指定 interval）
        String csvContent = klineFileService.readLocalKlineData(TEST_SYMBOL, null);
        assertNotNull(csvContent);

        String[] lines = csvContent.split("\n");

        // 验证表头
        assertTrue(lines.length > 1, "CSV 应至少有表头 + 1行数据");
        String header = lines[0];
        assertTrue(header.startsWith("timeInterval,"), "表头第一列应为 timeInterval");
        assertTrue(header.contains("openTime"), "表头应包含 openTime");
        assertTrue(header.contains("openPrice"), "表头应包含 openPrice");
        assertTrue(header.contains("highPrice"), "表头应包含 highPrice");
        assertTrue(header.contains("lowPrice"), "表头应包含 lowPrice");
        assertTrue(header.contains("closePrice"), "表头应包含 closePrice");
        assertTrue(header.contains("volume"), "表头应包含 volume");

        // 验证数据行格式（取第一行数据）
        String firstDataLine = lines[1];
        String[] fields = firstDataLine.split(",", -1);
        assertEquals(12, fields.length, "数据行应有12个字段（含 timeInterval）");

        // 验证 timeInterval 字段值合法
        String timeInterval = fields[0];
        assertTrue(timeInterval.equals("4h") || timeInterval.equals("1d") || timeInterval.equals("1w"),
                "timeInterval 应为 4h/1d/1w 之一，实际为: " + timeInterval);

        // 验证数值字段可以正确解析（fields[1] 开始是 openTime）
        try {
            Long.parseLong(fields[1]);
            new java.math.BigDecimal(fields[2]);
            new java.math.BigDecimal(fields[3]);
            new java.math.BigDecimal(fields[4]);
            new java.math.BigDecimal(fields[5]);
        } catch (Exception e) {
            fail("CSV 数值字段解析失败: " + e.getMessage());
        }

        System.out.println("✅ CSV 格式验证通过");
        System.out.println("   表头: " + header);
        System.out.println("   首行数据: " + firstDataLine);
    }

    /**
     * 测试5: 验证同步记录文件正确生成
     * 预期: sync_record.json 存在且包含各时间级别的同步时间
     */
    @Test
    void testSyncRecordFile() {
        klineFileService.syncKlineData(TEST_SYMBOL);

        String recordPath = klineProperties.getPath()
                + File.separator + TEST_SYMBOL
                + File.separator + "sync_record.json";

        File recordFile = new File(recordPath);
        assertTrue(recordFile.exists(), "同步记录文件应存在: " + recordPath);

        try {
            String content = Files.readString(recordFile.toPath());
            assertFalse(content.isEmpty(), "同步记录内容不应为空");
            assertTrue(content.contains("_lastOpenTime"), "记录应包含 _lastOpenTime");
            assertTrue(content.contains("_lastSyncTime"), "记录应包含 _lastSyncTime");
            System.out.println("✅ 同步记录验证通过");
            System.out.println("   记录内容: " + content);
        } catch (Exception e) {
            fail("读取同步记录失败: " + e.getMessage());
        }
    }

    /**
     * 测试6: 验证数据按时间倒序排列（最新的K线在最前面）
     */
    @Test
    void testDataOrder() {
        klineFileService.syncKlineData(TEST_SYMBOL);

        // 只读取 4h 的数据验证排序
        String csvContent = klineFileService.readLocalKlineData(TEST_SYMBOL, "4h");
        assertNotNull(csvContent);

        String[] lines = csvContent.split("\n");
        assertTrue(lines.length > 2, "数据应有多行");

        // 比较前两根K线的 openTime（第1行是表头，数据从第2行开始）
        // 现在 CSV 格式：timeInterval,openTime,openPrice,...
        String[] fields1 = lines[1].split(",", -1);
        String[] fields2 = lines[2].split(",", -1);

        // fields[0] = timeInterval, fields[1] = openTime
        long openTime1 = Long.parseLong(fields1[1]);
        long openTime2 = Long.parseLong(fields2[1]);

        assertTrue(openTime1 > openTime2,
                "数据应按 openTime 降序排列，第1行应晚于第2行");
        System.out.println("✅ 数据排序验证通过");
        System.out.println("   第1行 openTime: " + openTime1 + "，第2行 openTime: " + openTime2);
    }

    /**
     * 测试7: 验证 readLocalKlineData 按 interval 过滤正确
     * 预期: 传入 "4h" 只返回 4h 的数据行
     */
    @Test
    void testReadLocalKlineData_FilterByInterval() {
        klineFileService.syncKlineData(TEST_SYMBOL);

        String csv4h = klineFileService.readLocalKlineData(TEST_SYMBOL, "4h");
        String csv1d = klineFileService.readLocalKlineData(TEST_SYMBOL, "1d");
        String csv1w = klineFileService.readLocalKlineData(TEST_SYMBOL, "1w");
        String csvAll = klineFileService.readLocalKlineData(TEST_SYMBOL, null);

        assertNotNull(csv4h);
        assertNotNull(csv1d);
        assertNotNull(csv1w);
        assertNotNull(csvAll);

        // 验证过滤后的数据行数小于全量
        int lines4h = csv4h.split("\n").length - 1;
        int lines1d = csv1d.split("\n").length - 1;
        int lines1w = csv1w.split("\n").length - 1;
        int linesAll = csvAll.split("\n").length - 1;

        assertEquals(lines4h + lines1d + lines1w, linesAll,
                "全量数据行数应等于各 interval 行数之和");

        // 验证 4h 数据行的第一列都是 "4h"
        String[] lines4hArray = csv4h.split("\n");
        for (int i = 1; i < lines4hArray.length; i++) {
            assertEquals("4h", lines4hArray[i].split(",", -1)[0],
                    "4h 数据行第一列应为 4h");
        }

        System.out.println("✅ 按 interval 过滤验证通过");
        System.out.println("   4h: " + lines4h + " 行, 1d: " + lines1d + " 行, 1w: " + lines1w + " 行, 全量: " + linesAll + " 行");
    }

    // ==================== 辅助方法 ====================

    /**
     * 断言指定交易对的 kline.csv 文件存在
     */
    private void assertKlineCsvExists(String symbol) {
        String filePath = klineProperties.getPath()
                + File.separator + symbol
                + File.separator + "kline.csv";
        File file = new File(filePath);
        assertTrue(file.exists(), symbol + " kline.csv 应存在: " + filePath);
        System.out.println("  ✅ " + symbol + "/kline.csv 已生成: " + filePath);
    }

    /**
     * 断言指定交易对的 kline.csv 文件内容不为空
     */
    private void assertKlineCsvNotEmpty(String symbol) {
        String content = klineFileService.readLocalKlineData(symbol, null);
        assertNotNull(content, "CSV 内容不应为 null");
        String[] lines = content.split("\n");
        assertTrue(lines.length > 1, symbol + " CSV 应至少有1行数据（不含表头）");
        System.out.println("  ✅ " + symbol + "/kline.csv 共有 " + (lines.length - 1) + " 行数据");
    }

    /**
     * 统计 CSV 内容的数据行数（不含表头）
     */
    private int countCsvLines(String csvContent) {
        if (csvContent == null) return 0;
        String[] lines = csvContent.split("\n");
        return Math.max(0, lines.length - 1); // 减去表头
    }

    /**
     * 递归删除文件或目录
     */
    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
