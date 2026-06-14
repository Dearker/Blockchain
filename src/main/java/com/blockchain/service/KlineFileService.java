package com.blockchain.service;

import java.util.List;

/**
 * K线数据文件存储服务接口
 * 固定时间级别：4h、1d、1w
 * 固定数量：4h-300根、1d-180根、1w-100根
 *
 * @author blockchain
 */
public interface KlineFileService {

    /**
     * 同步指定交易对的K线数据到本地JSON文件
     * 支持增量拉取，保持固定数量的K线
     * 自动处理4h、1d、1w三个时间级别
     *
     * @param symbol 交易对符号，如：BTCUSDT
     */
    void syncKlineData(String symbol);

    /**
     * 同步多个交易对的K线数据到本地JSON文件
     *
     * @param symbols 交易对符号列表
     */
    void syncKlineData(List<String> symbols);

    /**
     * 读取本地JSON文件中的K线数据
     *
     * @param symbol 交易对符号
     * @param interval 时间级别
     * @return K线数据JSON字符串
     */
    String readLocalKlineData(String symbol, String interval);
}
