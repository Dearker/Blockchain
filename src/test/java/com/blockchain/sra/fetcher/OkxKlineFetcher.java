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
 * OKX K线数据拉取器
 * <p>
 * 从OKX公开REST API拉取K线数据并写入CSV文件。
 * 当Binance不可用时作为备选数据源。
 * <p>
 * API文档: https://www.okx.com/docs-v5/en/#order-book-trading-market-data-get-candlesticks
 * <p>
 * 使用示例:
 * <pre>
 *   OkxKlineFetcher fetcher = new OkxKlineFetcher();
 *   String csvPath = fetcher.fetchAndWriteCsv("BTC-USDT", "4H", 300, "/tmp/kline");
 * </pre>
 */
public class OkxKlineFetcher {

    private static final String DEFAULT_BASE_URL = "https://www.okx.com";

    private static final DateTimeFormatter CSV_DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String baseUrl;
    private int connectTimeout = 10000;
    private int readTimeout = 30000;
    private int maxRetries = 3;

    public OkxKlineFetcher() {
        this(DEFAULT_BASE_URL);
    }

    public OkxKlineFetcher(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * OKX interval参数映射
     * OKX格式: 1m, 3m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
     */
    private String mapInterval(String interval) {
        // OKX的interval是大写H/D/W，与Binance的小写不同
        String lower = interval.toLowerCase();
        switch (lower) {
            case "4h": return "4H";
            case "1d": return "1D";
            case "1w": return "1W";
            case "1h": return "1H";
            case "2h": return "2H";
            case "12h": return "12H";
            default: return interval.toUpperCase();
        }
    }

    /**
     * 将Binance格式的交易对转换为OKX格式
     * BTCUSDT → BTC-USDT, ETHUSDT → ETH-USDT
     */
    public static String convertSymbolToOkx(String binanceSymbol) {
        // 常见USDT交易对
        if (binanceSymbol.endsWith("USDT")) {
            return binanceSymbol.substring(0, binanceSymbol.length() - 4) + "-USDT";
        }
        if (binanceSymbol.endsWith("BUSD")) {
            return binanceSymbol.substring(0, binanceSymbol.length() - 4) + "-BUSD";
        }
        if (binanceSymbol.endsWith("BTC")) {
            return binanceSymbol.substring(0, binanceSymbol.length() - 3) + "-BTC";
        }
        if (binanceSymbol.endsWith("ETH")) {
            return binanceSymbol.substring(0, binanceSymbol.length() - 3) + "-ETH";
        }
        return binanceSymbol;
    }

    /**
     * 拉取K线原始数据
     *
     * @param instId   产品ID，如 BTC-USDT
     * @param bar      K线周期，如 4H, 1D, 1W
     * @param limit    拉取根数，最大300
     * @return 原始K线数据列表
     */
    public List<String[]> fetchRawKlines(String instId, String bar, int limit) throws IOException {
        String okxBar = mapInterval(bar);
        String urlStr = String.format("%s/api/v5/market/candles?instId=%s&bar=%s&limit=%d",
                baseUrl, instId, okxBar, Math.min(limit, 300));

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
                if (responseCode != 200) {
                    throw new IOException("API返回HTTP " + responseCode);
                }

                String response = readStream(conn.getInputStream());
                return parseOkxResponse(response);

            } catch (IOException e) {
                lastError = e;
                if (attempt < maxRetries) {
                    System.err.println("  OKX第" + attempt + "次请求失败: " + e.getMessage() + "，重试...");
                }
            }
        }
        throw new IOException("OKX请求失败(已重试" + maxRetries + "次): " + lastError.getMessage(), lastError);
    }

    /**
     * 拉取K线数据并写入CSV文件
     */
    public String fetchAndWriteCsv(String symbol, String interval, int limit, String outputDir) throws IOException {
        // 自动转换交易对格式
        String okxSymbol = symbol.contains("-") ? symbol : convertSymbolToOkx(symbol);
        List<String[]> rawData = fetchRawKlines(okxSymbol, interval, limit);

        File dir = new File(outputDir);
        if (!dir.exists()) dir.mkdirs();

        // 使用Binance格式的symbol作为文件名，保持一致性
        String fileName = String.format("%s_%s.csv", symbol.replace("-", ""), interval);
        String filePath = outputDir + File.separator + fileName;

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
            writer.write("date,open,high,low,close,volume\n");

            for (String[] row : rawData) {
                long openTime = Long.parseLong(row[0]);
                String date = Instant.ofEpochMilli(openTime)
                        .atZone(ZoneId.of("Asia/Shanghai"))
                        .format(CSV_DATE_FMT);

                writer.write(String.format("%s,%s,%s,%s,%s,%s\n",
                        date, row[1], row[2], row[3], row[4], row[5]));
            }
        }

        System.out.printf("  [OKX %s] 已写入 %d 根K线 → %s%n", interval, rawData.size(), filePath);
        return filePath;
    }

    /**
     * 拉取多时间级别K线数据并写入单个CSV文件
     * 添加一个timeframe列来区分不同的时间级别
     * 按模板回溯周期: 4H=300根、1D=180根、1W=100根
     * 注意: OKX单次最多300根
     *
     * @param symbol    交易对
     * @param outputDir CSV输出目录
     * @return 合并后的CSV文件路径
     */
    public String fetchMultiTimeFrameToSingleCsv(String symbol, String outputDir) throws IOException {
        // 自动转换交易对格式
        String okxSymbol = symbol.contains("-") ? symbol : convertSymbolToOkx(symbol);

        // 确保输出目录存在
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = String.format("%s_klines.csv", symbol.replace("-", ""));
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

                System.out.printf("  [OKX %s] 正在拉取数据...%n", interval);
                List<String[]> rawData = fetchRawKlines(okxSymbol, interval, limit);

                // 写入该时间级别的K线数据
                for (String[] row : rawData) {
                    long openTime = Long.parseLong(row[0]);
                    String date = Instant.ofEpochMilli(openTime)
                            .atZone(ZoneId.of("Asia/Shanghai"))
                            .format(CSV_DATE_FMT);

                    writer.write(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                            interval, date, row[1], row[2], row[3], row[4], row[5]));
                }

                System.out.printf("  [OKX %s] 已写入 %d 根K线%n", interval, rawData.size());
            }
        }

        System.out.printf("合并CSV文件已保存: %s%n", filePath);
        return filePath;
    }

    /**
     * 拉取多时间级别K线数据
     * 按模板回溯周期: 4H=300根、1D=180根、1W=100根
     * 注意: OKX单次最多300根，4H=300刚好满足，1D需分批(暂拉300)
     */
    public String[] fetchMultiTimeFrameCsv(String symbol, String outputDir) throws IOException {
        String csv4h = fetchAndWriteCsv(symbol, "4h", 300, outputDir);
        String csv1d = fetchAndWriteCsv(symbol, "1d", 180, outputDir);
        String csv1w = fetchAndWriteCsv(symbol, "1w", 100, outputDir);
        return new String[]{csv4h, csv1d, csv1w};
    }

    // ========== 内部方法 ==========

    /**
     * 解析OKX Kline API响应
     * <p>
     * OKX响应格式:
     * {
     *   "code": "0",
     *   "data": [
     *     ["ts", "o", "h", "l", "c", "vol", "volCcy", "volCcyQuote", "confirm"],
     *     ...
     *   ]
     * }
     * <p>
     * 注意：OKX返回数据是倒序的（最新在前），需要翻转
     */
    private List<String[]> parseOkxResponse(String json) {
        List<String[]> result = new ArrayList<>();

        // 简单提取 "data":[...] 部分
        int dataIdx = json.indexOf("\"data\"");
        if (dataIdx < 0) {
            throw new IllegalArgumentException("OKX响应缺少data字段: " + json.substring(0, Math.min(200, json.length())));
        }

        // 检查返回码
        int codeIdx = json.indexOf("\"code\"");
        if (codeIdx >= 0) {
            int quoteStart = json.indexOf("\"", codeIdx + 7);
            int quoteEnd = json.indexOf("\"", quoteStart + 1);
            String code = json.substring(quoteStart + 1, quoteEnd);
            if (!"0".equals(code)) {
                throw new IllegalArgumentException("OKX API返回错误码: " + code);
            }
        }

        // 找到data数组的起始
        int arrStart = json.indexOf('[', dataIdx + 6);
        int arrEnd = json.lastIndexOf(']');
        if (arrStart < 0 || arrEnd < 0) {
            throw new IllegalArgumentException("OKX响应data格式异常");
        }

        String dataJson = json.substring(arrStart, arrEnd + 1);

        // 解析内部数组
        int i = 1; // 跳过起始[
        while (i < dataJson.length() - 1) {
            while (i < dataJson.length() && (dataJson.charAt(i) == ',' || Character.isWhitespace(dataJson.charAt(i)))) i++;
            if (i >= dataJson.length() - 1) break;

            if (dataJson.charAt(i) != '[') break;
            int depth = 1;
            int start = i;
            i++;
            while (i < dataJson.length() && depth > 0) {
                char c = dataJson.charAt(i);
                if (c == '[') depth++;
                else if (c == ']') depth--;
                i++;
            }

            String innerArray = dataJson.substring(start + 1, i - 1);
            String[] fields = parseQuotedArray(innerArray);
            if (fields.length >= 6) {
                result.add(new String[]{
                        fields[0].trim(),  // ts (毫秒时间戳)
                        fields[1].trim(),  // open
                        fields[2].trim(),  // high
                        fields[3].trim(),  // low
                        fields[4].trim(),  // close
                        fields[5].trim(),  // volume
                });
            }
        }

        // OKX返回倒序，翻转
        java.util.Collections.reverse(result);
        return result;
    }

    /**
     * 解析OKX内层数组（全部是带引号的字符串）
     */
    private String[] parseQuotedArray(String inner) {
        List<String> fields = new ArrayList<>();
        int i = 0;
        while (i < inner.length()) {
            while (i < inner.length() && Character.isWhitespace(inner.charAt(i))) i++;
            if (i >= inner.length()) break;

            if (inner.charAt(i) == '"') {
                int start = ++i;
                while (i < inner.length() && inner.charAt(i) != '"') i++;
                fields.add(inner.substring(start, i));
                i++;
            } else {
                int start = i;
                while (i < inner.length() && inner.charAt(i) != ',') i++;
                fields.add(inner.substring(start, i).trim());
            }

            if (i < inner.length() && inner.charAt(i) == ',') i++;
        }
        return fields.toArray(new String[0]);
    }

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

    public void setConnectTimeout(int connectTimeout) { this.connectTimeout = connectTimeout; }
    public void setReadTimeout(int readTimeout) { this.readTimeout = readTimeout; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
}
