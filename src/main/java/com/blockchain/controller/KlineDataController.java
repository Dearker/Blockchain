package com.blockchain.controller;

import com.blockchain.service.KlineDataService;
import com.blockchain.service.KlineFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * K线数据拉取控制器
 * 只需输入交易对名称，自动处理4h、1d、1w三个时间级别
 *
 * @author blockchain
 */
@Slf4j
@RestController
@RequestMapping("/kline")
@RequiredArgsConstructor
public class KlineDataController {

    private final KlineDataService klineDataService;
    private final KlineFileService klineFileService;

    /**
     * 同步K线数据到MySQL
     * 只需传入交易对名称，自动拉取4h、1d、1w三个时间级别的数据
     *
     * @param symbols 交易对符号，多个用逗号分隔，如：BTCUSDT,ETHUSDT
     * @return 同步结果
     */
    @GetMapping("/sync/mysql")
    public String syncToMysql(@RequestParam String symbols) {
        try {
            List<String> symbolList = Arrays.asList(symbols.split(","));
            klineDataService.syncKlineData(symbolList);
            return "MySQL数据同步成功，交易对：" + symbols + "，已自动拉取4h(300根)、1d(180根)、1w(100根)";
        } catch (Exception e) {
            log.error("MySQL数据同步失败", e);
            return "MySQL数据同步失败：" + e.getMessage();
        }
    }

    /**
     * 同步K线数据到本地文件
     * 只需传入交易对名称，自动拉取4h、1d、1w三个时间级别的数据
     *
     * @param symbols 交易对符号，多个用逗号分隔，如：BTCUSDT,ETHUSDT
     * @return 同步结果
     */
    @GetMapping("/sync/file")
    public String syncToFile(@RequestParam String symbols) {
        try {
            List<String> symbolList = Arrays.asList(symbols.split(","));
            klineFileService.syncKlineData(symbolList);
            return "文件数据同步成功，交易对：" + symbols + "，已自动拉取4h(300根)、1d(180根)、1w(100根)";
        } catch (Exception e) {
            log.error("文件数据同步失败", e);
            return "文件数据同步失败：" + e.getMessage();
        }
    }

    /**
     * 同时同步到MySQL和文件
     * 只需传入交易对名称，自动拉取4h、1d、1w三个时间级别的数据
     *
     * @param symbols 交易对符号，多个用逗号分隔，如：BTCUSDT,ETHUSDT
     * @return 同步结果
     */
    @GetMapping("/sync/all")
    public String syncToAll(@RequestParam String symbols) {
        try {
            List<String> symbolList = Arrays.asList(symbols.split(","));
            klineDataService.syncKlineData(symbolList);
            klineFileService.syncKlineData(symbolList);
            return "MySQL和文件数据同步成功，交易对：" + symbols + "，已自动拉取4h(300根)、1d(180根)、1w(100根)";
        } catch (Exception e) {
            log.error("数据同步失败", e);
            return "数据同步失败：" + e.getMessage();
        }
    }

    /**
     * 读取本地K线数据文件
     *
     * @param symbol 交易对符号
     * @param interval 时间级别
     * @return K线数据JSON字符串
     */
    @GetMapping("/read")
    public String readLocalData(@RequestParam String symbol, @RequestParam String interval) {
        return klineFileService.readLocalKlineData(symbol, interval);
    }
}
