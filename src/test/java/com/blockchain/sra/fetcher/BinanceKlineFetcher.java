package com.blockchain.sra.fetcher;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 币安(Binance) K线数据拉取器
 * <p>
 * 从Binance合约(USDT-M Futures)公开REST API拉取K线数据并写入CSV文件，供支撑阻力位分析工具使用。
 * 无需API Key，使用公开端点。
 * <p>
 * API文档: https://binance-docs.github.io/apidocs/futures/en/#kline-candlestick-data
 * <p>
 * 模板指定地址: https://fapi.binance.com/fapi/v1/klines?symbol={交易对}&interval={时间级别}&limit=500
 * <p>
 * 支持的interval: 1m, 3m, 5m, 15m, 30m, 1h, 2h, 4h, 6h, 8h, 12h, 1d, 3d, 1w, 1M
 * <p>
 * 使用示例:
 * <pre>
 *   BinanceKlineFetcher fetcher = new BinanceKlineFetcher();
 *   String csvPath = fetcher.fetchAndWriteCsv("BTCUSDT", "4h", 300, "/tmp/kline");
 * </pre>
 */
public class BinanceKlineFetcher {

    /** 默认API基础URL（合约USDT-M Futures API，与模板一致） */
    private static final String DEFAULT_BASE_URL = "https://fapi.binance.com";

    /** 备用URL列表（国内网络可能需要） */
    private static final String[] FALLBACK_URLS = {
            "https://fapi1.binance.com",
            "https://fapi2.binance.com",
            "https://fapi3.binance.com",
    };

    private static final DateTimeFormatter CSV_DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** API基础URL */
    private String baseUrl;

    /** 请求超时（毫秒） */
    private int connectTimeout = 10000;

    /** 读取超时（毫秒） */
    private int readTimeout = 30000;

    /** 最大重试次数 */
    private int maxRetries = 3;

    public BinanceKlineFetcher() {
        this(DEFAULT_BASE_URL);
    }

    public BinanceKlineFetcher(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    // ========== 核心方法 ==========

    /**
     * 拉取K线原始数据
     *
     * @param symbol   交易对，如 BTCUSDT, ETHUSDT
     * @param interval K线周期，如 4h, 1d, 1w
     * @param limit    拉取根数，最大1000
     * @return 原始K线数据列表，每行为 [openTime, open, high, low, close, volume, closeTime]
     */
    public List<String[]> fetchRawKlines(String symbol, String interval, int limit) throws IOException {
        IOException lastError = null;

        // 先尝试主URL
        try {
            return doFetch(symbol, interval, limit, baseUrl);
        } catch (IOException e) {
            lastError = e;
            System.err.println("主节点请求失败: " + e.getMessage() + "，尝试备用节点...");
        }

        // 依次尝试备用URL
        for (String fallback : FALLBACK_URLS) {
            try {
                System.err.println("尝试备用节点: " + fallback);
                List<String[]> result = doFetch(symbol, interval, limit, fallback);
                // 成功后更新baseUrl为可用节点
                this.baseUrl = fallback;
                return result;
            } catch (IOException e) {
                lastError = e;
                System.err.println("备用节点 " + fallback + " 失败: " + e.getMessage());
            }
        }

        throw new IOException("所有API节点均不可用，最后错误: " + lastError.getMessage(), lastError);
    }

    /**
     * 拉取K线数据并写入CSV文件
     *
     * @param symbol     交易对
     * @param interval   K线周期
     * @param limit      拉取根数
     * @param outputDir  CSV输出目录
     * @return 生成的CSV文件路径
     */
    public String fetchAndWriteCsv(String symbol, String interval, int limit, String outputDir) throws IOException {
        List<String[]> rawData = fetchRawKlines(symbol, interval, limit);

        // 确保输出目录存在
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = String.format("%s_%s.csv", symbol, interval);
        String filePath = outputDir + File.separator + fileName;

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
            // 写表头
            writer.write("date,open,high,low,close,volume\n");

            // 写数据行
            for (String[] row : rawData) {
                long openTime = Long.parseLong(row[0]);
                String date = Instant.ofEpochMilli(openTime)
                        .atZone(ZoneId.of("Asia/Shanghai"))
                        .format(CSV_DATE_FMT);

                writer.write(String.format("%s,%s,%s,%s,%s,%s\n",
                        date, row[1], row[2], row[3], row[4], row[5]));
            }
        }

        System.out.printf("  [%s] 已写入 %d 根K线 → %s%n", interval, rawData.size(), filePath);
        return filePath;
    }

    /**
     * 拉取多时间级别K线数据并写入单个CSV文件
     * 添加一个timeframe列来区分不同的时间级别
     * 按模板回溯周期: 4H=300根、1D=180根、1W=100根
     *
     * @param symbol    交易对
     * @param outputDir CSV输出目录
     * @return 合并后的CSV文件路径
     */
    public String fetchMultiTimeFrameToSingleCsv(String symbol, String outputDir) throws IOException {
        // 确保输出目录存在
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = String.format("%s_klines.csv", symbol);
        String filePath = outputDir + File.separator + fileName;

        // 定义时间级别和对应的数据量
        String[][] timeFrames = {
                {"4h", "300"},
                {"1d", "180"},
                {"1w", "100"}
        };

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
            // 写表头（添加timeframe列）
            writer.write("timeframe,date,open,high,low,close,volume\n");

            // 逐个时间级别写入
            for (String[] tf : timeFrames) {
                String interval = tf[0];
                int limit = Integer.parseInt(tf[1]);

                System.out.printf("  [%s] 正在拉取数据...%n", interval);
                List<String[]> rawData = fetchRawKlines(symbol, interval, limit);

                // 写入该时间级别的K线数据
                for (String[] row : rawData) {
                    long openTime = Long.parseLong(row[0]);
                    String date = Instant.ofEpochMilli(openTime)
                            .atZone(ZoneId.of("Asia/Shanghai"))
                            .format(CSV_DATE_FMT);

                    writer.write(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                            interval, date, row[1], row[2], row[3], row[4], row[5]));
                }

                System.out.printf("  [%s] 已写入 %d 根K线%n", interval, rawData.size());
            }
        }

        System.out.printf("合并CSV文件已保存: %s%n", filePath);
        return filePath;
    }

    /**
     * 拉取多时间级别K线数据并写入CSV
     * 按模板回溯周期: 4H=300根、1D=180根、1W=100根
     *
     * @param symbol    交易对
     * @param outputDir CSV输出目录
     * @return [4H CSV路径, 1D CSV路径, 1W CSV路径]
     */
    public String[] fetchMultiTimeFrameCsv(String symbol, String outputDir) throws IOException {
        String csv4h = fetchAndWriteCsv(symbol, "4h", 300, outputDir);
        String csv1d = fetchAndWriteCsv(symbol, "1d", 180, outputDir);
        String csv1w = fetchAndWriteCsv(symbol, "1w", 100, outputDir);
        return new String[]{csv4h, csv1d, csv1w};
    }

    // ========== 内部方法 ==========

    /**
     * 执行单次API请求
     */
    private List<String[]> doFetch(String symbol, String interval, int limit, String baseUrl) throws IOException {
        String urlStr = String.format("%s/fapi/v1/klines?symbol=%s&interval=%s&limit=%d",
                baseUrl, symbol.toUpperCase(), interval, Math.min(limit, 1500));

        IOException lastError = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(connectTimeout);
                conn.setReadTimeout(readTimeout);
                conn.setRequestProperty("User-Agent", "SRA/1.0");

                int responseCode = conn.getResponseCode();
                if (responseCode == 429) {
                    // 限频，等待后重试
                    int waitSec = attempt * 3;
                    System.err.println("  请求限频，等待 " + waitSec + " 秒后重试...");
                    Thread.sleep(waitSec * 1000L);
                    continue;
                }
                if (responseCode != 200) {
                    String errorBody = readStream(conn.getErrorStream());
                    throw new IOException("API返回HTTP " + responseCode + ": " + errorBody);
                }

                String response = readStream(conn.getInputStream());
                return parseKlineResponse(response);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("请求被中断", e);
            } catch (IOException e) {
                lastError = e;
                if (attempt < maxRetries) {
                    System.err.println("  第" + attempt + "次请求失败: " + e.getMessage() + "，重试...");
                }
            }
        }
        throw new IOException("请求失败(已重试" + maxRetries + "次): " + lastError.getMessage(), lastError);
    }

    /**
     * 读取输入流
     */
    private String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    /**
     * 解析Binance Kline API响应
     * <p>
     * 响应格式:
     * [
     *   [openTime, open, high, low, close, volume, closeTime, quoteVolume, trades, ...],
     *   ...
     * ]
     * <p>
     * 简化解析：不依赖JSON库，直接按结构拆分
     */
    private List<String[]> parseKlineResponse(String json) {
        List<String[]> result = new ArrayList<>();

        // 去除首尾空白
        json = json.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) {
            throw new IllegalArgumentException("无效的API响应格式");
        }

        // 去除外层数组括号
        json = json.substring(1, json.length() - 1).trim();

        // 逐个解析内部数组 [...], [...], ...
        int i = 0;
        while (i < json.length()) {
            // 跳过逗号和空白
            while (i < json.length() && (json.charAt(i) == ',' || Character.isWhitespace(json.charAt(i)))) {
                i++;
            }
            if (i >= json.length()) break;

            // 找到内层数组的 [...]
            if (json.charAt(i) != '[') break;
            int depth = 1;
            int start = i;
            i++;
            while (i < json.length() && depth > 0) {
                char c = json.charAt(i);
                if (c == '[') depth++;
                else if (c == ']') depth--;
                i++;
            }

            String innerArray = json.substring(start + 1, i - 1);
            String[] fields = parseInnerArray(innerArray);
            if (fields.length >= 6) {
                // 只取前6个字段: openTime, open, high, low, close, volume
                result.add(new String[]{
                        fields[0].trim(),  // openTime
                        fields[1].trim(),  // open
                        fields[2].trim(),  // high
                        fields[3].trim(),  // low
                        fields[4].trim(),  // close
                        fields[5].trim(),  // volume
                });
            }
        }

        return result;
    }

    /**
     * 解析内层数组字符串
     * 格式: 1499040000000,"0.0163","0.8000","0.0157","0.0157","148976.11",...
     */
    private String[] parseInnerArray(String inner) {
        List<String> fields = new ArrayList<>();
        int i = 0;
        while (i < inner.length()) {
            // 跳过空白
            while (i < inner.length() && Character.isWhitespace(inner.charAt(i))) i++;
            if (i >= inner.length()) break;

            if (inner.charAt(i) == '"') {
                // 带引号的字符串
                int start = ++i;
                while (i < inner.length() && inner.charAt(i) != '"') i++;
                fields.add(inner.substring(start, i));
                i++; // 跳过结束引号
            } else {
                // 不带引号的数字
                int start = i;
                while (i < inner.length() && inner.charAt(i) != ',') i++;
                fields.add(inner.substring(start, i).trim());
            }

            // 跳过逗号
            if (i < inner.length() && inner.charAt(i) == ',') i++;
        }
        return fields.toArray(new String[0]);
    }

    // ========== Getter & Setter ==========

    public void setConnectTimeout(int connectTimeout) { this.connectTimeout = connectTimeout; }
    public void setReadTimeout(int readTimeout) { this.readTimeout = readTimeout; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public String getBaseUrl() { return baseUrl; }
}
