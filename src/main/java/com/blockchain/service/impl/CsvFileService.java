package com.blockchain.service.impl;

import com.opencsv.CSVWriter;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Service
public class CsvFileService {

    /**
     * 将数据写入本地 CSV 文件
     *
     * @param filePath CSV 文件路径（如：/data/output.csv）
     * @param data     要写入的数据（每一行是一个字符串数组）
     */
    private void writeDataToCsv(String filePath, List<String[]> data) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            // 写入数据
            writer.writeAll(data);
            System.out.println("数据已成功写入 CSV 文件: " + filePath);
        } catch (IOException e) {
            System.err.println("写入 CSV 文件时发生错误: " + e.getMessage());
        }
    }

    /**
     * 示例：生成测试数据并写入 CSV 文件
     */
    public void generateSampleCsv(String bar,List<String[]> data) {
        // 表头
        String[] header = {"开盘时间", "开盘价", "最高价","最低价","收盘价","成交量","成交额"};
        data.addFirst(header);

        String filePath = "/Volumes/husky/temp/data/output_" + bar + "_" + System.currentTimeMillis() + ".csv";
        // 写入 CSV 文件
        this.writeDataToCsv(filePath, data);
    }

}
