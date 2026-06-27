package com.blockchain.sra;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Binance K线CSV拉取测试
 * <p>
 * 参考 {@link RealDataIntegrationTest} 中的真实数据拉取入口，将 Binance K线数据按多个时间级别
 * 合并写入一个CSV文件。每个测试方法都可通过 JUnit 的 @Test 单独运行。
 */
public class BinanceKlineCsvFetchTest {

    /** 默认交易对 */
    private static final String DEFAULT_SYMBOL = "BTCUSDT";

    /** CSV输出目录 */
    private static final String DEFAULT_OUTPUT_DIR = "/Volumes/husky/workspace/Blockchain/doc/kdata/lines/" + DEFAULT_SYMBOL;

    /** Binance USDT-M Futures API */
    private static final String DEFAULT_BASE_URL = "https://fapi.binance.com";

    /** 单次K线请求最大数量，Binance Futures klines接口上限为1500 */
    private static final int BINANCE_MAX_LIMIT = 1500;

    private static final ZoneId CSV_ZONE = ZoneId.of("Asia/Shanghai");

    private static final DateTimeFormatter CSV_DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 自定义时间级别和K线数量。
     * <p>
     * 支持任意组合: 1h、4h、1d、1w、1M(月)。
     */
    private static final Map<String, Integer> TIME_FRAME_LIMITS = new LinkedHashMap<>() {{
        put("1h", 120);
        put("4h", 300);
        put("1d", 180);
        put("1w", 100);
        put("1M", 36);
    }};

    /**
     * 测试1: 按配置的多个时间级别拉取K线，并合并写入一个CSV文件。
     */
    @Test
    void testFetchCustomTimeFramesToSingleCsv() throws IOException {
        String csvPath = fetchMergedKlineData(DEFAULT_SYMBOL, TIME_FRAME_LIMITS, DEFAULT_OUTPUT_DIR);

        File csvFile = new File(csvPath);
        assertTrue(csvFile.exists(), "CSV文件应存在: " + csvPath);
        assertTrue(csvFile.length() > 0, "CSV文件不应为空: " + csvPath);

        System.out.println("合并CSV文件已生成: " + csvPath);
    }

    /**
     * 测试2: 验证开始时间会根据指定K线数量从当前结束时间自动倒推。
     */
    @Test
    void testCalculateStartTimeByLimit() {
        ZonedDateTime endTime = ZonedDateTime.of(2026, 6, 27, 12, 0, 0, 0, CSV_ZONE);

        assertEquals(endTime.minusHours(10), calculateStartTime(endTime, "1h", 10));
        assertEquals(endTime.minusHours(40), calculateStartTime(endTime, "4h", 10));
        assertEquals(endTime.minusDays(10), calculateStartTime(endTime, "1d", 10));
        assertEquals(endTime.minusWeeks(10), calculateStartTime(endTime, "1w", 10));
        assertEquals(endTime.minusMonths(10), calculateStartTime(endTime, "1M", 10));
    }

    /**
     * 从币安拉取多个时间级别的K线数据并合并到单个CSV文件。
     * <p>
     * 该入口参考 RealDataIntegrationTest#fetchMergedKlineData 的拉取流程，当前测试类只使用 Binance。
     */
    private static String fetchMergedKlineData(String symbol,
                                               Map<String, Integer> timeFrameLimits,
                                               String outputDir) throws IOException {
        if (timeFrameLimits == null || timeFrameLimits.isEmpty()) {
            throw new IllegalArgumentException("timeFrameLimits 不能为空");
        }

        File dir = new File(outputDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("创建输出目录失败: " + outputDir);
        }

        String fileName = String.format("%s_custom_klines.csv", symbol.toUpperCase());
        String filePath = outputDir + File.separator + fileName;
        ZonedDateTime endTime = ZonedDateTime.now(CSV_ZONE);

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new java.io.FileOutputStream(filePath), StandardCharsets.UTF_8))) {
            writer.write("timeframe,date,open,high,low,close,volume\n");

            for (Map.Entry<String, Integer> entry : timeFrameLimits.entrySet()) {
                String interval = entry.getKey();
                int limit = entry.getValue();
                validateInterval(interval);
                validateLimit(limit);

                ZonedDateTime startTime = calculateStartTime(endTime, interval, limit);
                System.out.printf("  [%s] 拉取 %d 根K线，时间范围: %s ~ %s%n",
                        interval,
                        limit,
                        startTime.format(CSV_DATE_FMT),
                        endTime.format(CSV_DATE_FMT));

                List<String[]> rawData = fetchRawKlines(symbol, interval, limit,
                        startTime.toInstant().toEpochMilli(),
                        endTime.toInstant().toEpochMilli());

                assertNotNull(rawData, interval + " 数据不应为 null");
                assertFalse(rawData.isEmpty(), interval + " 数据不应为空");

                for (String[] row : rawData) {
                    long openTime = Long.parseLong(row[0]);
                    String date = Instant.ofEpochMilli(openTime)
                            .atZone(CSV_ZONE)
                            .format(CSV_DATE_FMT);

                    writer.write(String.format("%s,%s,%s,%s,%s,%s,%s%n",
                            interval, date, row[1], row[2], row[3], row[4], row[5]));
                }

                System.out.printf("  [%s] 已写入 %d 根K线%n", interval, rawData.size());
            }
        }

        return filePath;
    }

    /**
     * 拉取指定时间范围内的K线原始数据。
     *
     * @return 每行为 [openTime, open, high, low, close, volume, closeTime]
     */
    private static List<String[]> fetchRawKlines(String symbol,
                                                 String interval,
                                                 int limit,
                                                 long startTime,
                                                 long endTime) throws IOException {
        List<String[]> result = new ArrayList<>();
        long cursor = startTime;

        while (result.size() < limit && cursor <= endTime) {
            int requestLimit = Math.min(BINANCE_MAX_LIMIT, limit - result.size());
            List<String[]> batch = doFetch(symbol, interval, requestLimit, cursor, endTime);
            if (batch.isEmpty()) {
                break;
            }

            for (String[] row : batch) {
                if (result.size() >= limit) {
                    break;
                }
                result.add(row);
            }

            long lastCloseTime = Long.parseLong(batch.get(batch.size() - 1)[6]);
            long nextCursor = lastCloseTime + 1;
            if (nextCursor <= cursor) {
                break;
            }
            cursor = nextCursor;
        }

        return result;
    }

    /**
     * 执行单次 Binance Kline API 请求。
     */
    private static List<String[]> doFetch(String symbol,
                                          String interval,
                                          int limit,
                                          long startTime,
                                          long endTime) throws IOException {
        String urlStr = String.format("%s/fapi/v1/klines?symbol=%s&interval=%s&startTime=%d&endTime=%d&limit=%d",
                DEFAULT_BASE_URL,
                symbol.toUpperCase(),
                interval,
                startTime,
                endTime,
                Math.min(limit, BINANCE_MAX_LIMIT));

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("User-Agent", "Blockchain-Test/1.0");

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            String errorBody = readStream(conn.getErrorStream());
            throw new IOException("Binance API返回HTTP " + responseCode + ": " + errorBody);
        }

        return parseKlineResponse(readStream(conn.getInputStream()));
    }

    /**
     * 解析 Binance Kline API 响应。
     */
    private static List<String[]> parseKlineResponse(String json) {
        JSONArray array = JSON.parseArray(json);
        List<String[]> result = new ArrayList<>();

        for (int i = 0; i < array.size(); i++) {
            JSONArray row = array.getJSONArray(i);
            result.add(new String[]{
                    row.get(0).toString(),
                    row.get(1).toString(),
                    row.get(2).toString(),
                    row.get(3).toString(),
                    row.get(4).toString(),
                    row.get(5).toString(),
                    row.get(6).toString()
            });
        }

        return result;
    }

    /**
     * 根据K线数量，以当前时间作为结束时间自动计算开始时间。
     */
    private static ZonedDateTime calculateStartTime(ZonedDateTime endTime, String interval, int limit) {
        return switch (interval) {
            case "1h" -> endTime.minusHours(limit);
            case "4h" -> endTime.minusHours(4L * limit);
            case "1d" -> endTime.minusDays(limit);
            case "1w" -> endTime.minusWeeks(limit);
            case "1M" -> endTime.minusMonths(limit);
            default -> throw new IllegalArgumentException("不支持的时间级别: " + interval);
        };
    }

    private static void validateInterval(String interval) {
        if (!List.of("1h", "4h", "1d", "1w", "1M").contains(interval)) {
            throw new IllegalArgumentException("仅支持 1h、4h、1d、1w、1M，实际为: " + interval);
        }
    }

    private static void validateLimit(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("K线数量必须大于0，实际为: " + limit);
        }
    }

    private static String readStream(InputStream is) throws IOException {
        if (is == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
