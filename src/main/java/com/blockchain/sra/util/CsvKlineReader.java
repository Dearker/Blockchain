package com.blockchain.sra.util;

import com.blockchain.sra.model.Kline;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV文件读取器
 * 支持多种常见K线CSV格式:
 * 1. date,open,high,low,close,volume
 * 2. datetime,open,high,low,close,volume
 * 3. 币安导出格式: open_time,open,high,low,close,volume,...
 *
 * 日期格式自动检测: yyyy-MM-dd / yyyy-MM-dd HH:mm:ss / yyyy/MM/dd 等
 */
public class CsvKlineReader {

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyyMMdd"),
    };

    /**
     * 从CSV文件读取K线数据
     *
     * @param filePath CSV文件路径
     * @return K线数据列表（按时间升序）
     */
    public static List<Kline> read(String filePath) throws IOException {
        return read(filePath, true);
    }

    /**
     * 从CSV文件读取K线数据
     *
     * @param filePath  CSV文件路径
     * @param hasHeader 是否有表头行
     * @return K线数据列表（按时间升序）
     */
    public static List<Kline> read(String filePath, boolean hasHeader) throws IOException {
        List<Kline> klines = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> rows;
            try {
                rows = reader.readAll();
            } catch (CsvException e) {
                throw new IOException("CSV解析失败: " + e.getMessage(), e);
            }

            int startIdx = hasHeader ? 1 : 0;
            for (int i = startIdx; i < rows.size(); i++) {
                String[] cols = rows.get(i);
                if (cols.length < 6) continue;

                // 跳过空行
                if (cols[0] == null || cols[0].trim().isEmpty()) continue;

                try {
                    Kline k = parseRow(cols);
                    if (k != null) {
                        klines.add(k);
                    }
                } catch (Exception e) {
                    // 跳过解析失败的行
                    System.err.printf("第%d行解析失败: %s%n", i + 1, e.getMessage());
                }
            }
        }

        // 按时间升序排列
        klines.sort((a, b) -> a.getDatetime().compareTo(b.getDatetime()));
        return klines;
    }

    /**
     * 从CSV文件读取K线数据，限制回溯根数
     *
     * @param filePath  CSV文件路径
     * @param lookback  回溯根数
     * @return K线数据列表
     */
    public static List<Kline> readWithLookback(String filePath, int lookback) throws IOException {
        List<Kline> all = read(filePath);
        if (all.size() <= lookback) {
            return all;
        }
        return all.subList(all.size() - lookback, all.size());
    }

    /**
     * 从合并的CSV文件读取指定时间级别的K线数据
     * 合并CSV格式: timeframe,date,open,high,low,close,volume
     *
     * @param filePath 合并的CSV文件路径
     * @param timeframe 时间级别，如 "4h", "1d", "1w"
     * @return K线数据列表（按时间升序）
     */
    public static List<Kline> readFromMerged(String filePath, String timeframe) throws IOException {
        return readFromMerged(filePath, timeframe, true);
    }

    /**
     * 从合并的CSV文件读取指定时间级别的K线数据
     *
     * @param filePath 合并的CSV文件路径
     * @param timeframe 时间级别，如 "4h", "1d", "1w"
     * @param hasHeader 是否有表头行
     * @return K线数据列表（按时间升序）
     */
    public static List<Kline> readFromMerged(String filePath, String timeframe, boolean hasHeader) throws IOException {
        List<Kline> klines = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> rows;
            try {
                rows = reader.readAll();
            } catch (CsvException e) {
                throw new IOException("CSV解析失败: " + e.getMessage(), e);
            }

            // 确定timeframe列和date列的索引
            int timeframeCol = -1;
            int startIdx = hasHeader ? 1 : 0;

            if (hasHeader && rows.size() > 0) {
                String[] header = rows.get(0);
                for (int i = 0; i < header.length; i++) {
                    if ("timeframe".equalsIgnoreCase(header[i].trim())) {
                        timeframeCol = i;
                        break;
                    }
                }
                if (timeframeCol < 0) {
                    throw new IOException("CSV文件缺少timeframe列，无法按时间级别过滤");
                }
            }

            for (int i = startIdx; i < rows.size(); i++) {
                String[] cols = rows.get(i);
                if (cols.length < 6) continue;

                // 跳过空行
                if (cols[0] == null || cols[0].trim().isEmpty()) continue;

                // 检查timeframe列（第一列）是否匹配
                String rowTimeframe = cols[0].trim();
                if (!rowTimeframe.equalsIgnoreCase(timeframe)) {
                    continue;
                }

                try {
                    Kline k = parseRowMerged(cols);
                    if (k != null) {
                        klines.add(k);
                    }
                } catch (Exception e) {
                    // 跳过解析失败的行
                    System.err.printf("第%d行解析失败: %s%n", i + 1, e.getMessage());
                }
            }
        }

        // 按时间升序排列
        klines.sort((a, b) -> a.getDatetime().compareTo(b.getDatetime()));
        return klines;
    }

    /**
     * 从合并的CSV文件读取指定时间级别的K线数据，限制回溯根数
     *
     * @param filePath 合并的CSV文件路径
     * @param timeframe 时间级别，如 "4h", "1d", "1w"
     * @param lookback 回溯根数
     * @return K线数据列表
     */
    public static List<Kline> readFromMergedWithLookback(String filePath, String timeframe, int lookback) throws IOException {
        List<Kline> all = readFromMerged(filePath, timeframe);
        if (all.size() <= lookback) {
            return all;
        }
        return all.subList(all.size() - lookback, all.size());
    }

    /**
     * 解析合并CSV的行数据（第一列为timeframe）
     */
    private static Kline parseRowMerged(String[] cols) {
        // cols[0] = timeframe, cols[1] = date, cols[2] = open, ...
        LocalDateTime datetime = parseDateTime(cols[1].trim());
        double open = parseDouble(cols[2].trim());
        double high = parseDouble(cols[3].trim());
        double low = parseDouble(cols[4].trim());
        double close = parseDouble(cols[5].trim());
        double volume = cols.length > 6 ? parseDouble(cols[6].trim()) : 0.0;

        if (Double.isNaN(open) || Double.isNaN(high) || Double.isNaN(low) || Double.isNaN(close)) {
            return null;
        }

        return new Kline(datetime, open, high, low, close, volume);
    }

    /**
     * 解析单行CSV数据
     */
    private static Kline parseRow(String[] cols) {
        LocalDateTime datetime = parseDateTime(cols[0].trim());
        double open = parseDouble(cols[1].trim());
        double high = parseDouble(cols[2].trim());
        double low = parseDouble(cols[3].trim());
        double close = parseDouble(cols[4].trim());
        double volume = parseDouble(cols[5].trim());

        if (Double.isNaN(open) || Double.isNaN(high) || Double.isNaN(low) || Double.isNaN(close)) {
            return null;
        }

        return new Kline(datetime, open, high, low, close, volume);
    }

    /**
     * 解析日期时间，自动检测格式
     */
    private static LocalDateTime parseDateTime(String dateStr) {
        // 尝试纯数字时间戳（毫秒）
        try {
            long ts = Long.parseLong(dateStr);
            if (ts > 1_000_000_000_000L) {
                return LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(ts),
                        java.time.ZoneId.systemDefault());
            }
        } catch (NumberFormatException ignored) {}

        // 尝试各种日期格式
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDateTime.parse(dateStr, fmt);
            } catch (Exception ignored) {}
        }

        // 尝试纯日期格式 → 转为当天 00:00
        // yyyy/MM/dd
        try {
            LocalDate ld = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            return ld.atStartOfDay();
        } catch (Exception ignored) {}

        // yyyy-MM-dd
        try {
            LocalDate ld = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return ld.atStartOfDay();
        } catch (Exception ignored) {}

        // 尝试纯年月格式 yyyy-MM → 转为该月1号 00:00
        try {
            if (dateStr.matches("\\d{4}-\\d{2}")) {
                LocalDate ld = LocalDate.parse(dateStr + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                return ld.atStartOfDay();
            }
        } catch (Exception ignored) {}

        // 尝试ISO格式
        try {
            return LocalDateTime.parse(dateStr);
        } catch (Exception ignored) {}

        throw new IllegalArgumentException("无法解析日期: " + dateStr);
    }

    /**
     * 安全解析Double
     */
    private static double parseDouble(String val) {
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
