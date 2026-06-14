package com.blockchain.constants;

import java.util.HashMap;
import java.util.Map;

/**
 * 币安API常量类
 * 路径配置已迁移至 {@link com.blockchain.config.KlineProperties}
 * 此类只保留 API URL 和间隔配置等纯常量
 *
 * @author blockchain
 */
public class BinanceApiConstants {

    /**
     * 币安合约API基础地址
     */
    public static final String BINANCE_FAPI_BASE_URL = "https://fapi.binance.com";

    /**
     * K线数据API路径
     */
    public static final String KLINES_API_PATH = "/fapi/v1/klines";

    /**
     * 完整K线API地址
     */
    public static final String KLINES_API_URL = BINANCE_FAPI_BASE_URL + KLINES_API_PATH;

    /**
     * 存储类型 - MySQL
     */
    public static final String STORAGE_TYPE_MYSQL = "mysql";

    /**
     * 存储类型 - 文件
     */
    public static final String STORAGE_TYPE_FILE = "file";

    /**
     * K线间隔配置
     * key: 时间级别
     * value: 目标K线数量
     */
    public static final Map<String, Integer> INTERVAL_TARGET_COUNT = new HashMap<>();

    static {
        // 4小时级别，目标300根K线
        INTERVAL_TARGET_COUNT.put("4h", 300);
        // 1天级别，目标180根K线
        INTERVAL_TARGET_COUNT.put("1d", 180);
        // 1周级别，目标100根K线
        INTERVAL_TARGET_COUNT.put("1w", 100);
    }

    /**
     * 支持的时间级别
     */
    public static final String[] SUPPORTED_INTERVALS = {"4h", "1d", "1w"};

    /**
     * 同步记录文件名称
     */
    public static final String SYNC_RECORD_FILE_NAME = "sync_record.json";

    /**
     * API请求每次最大返回数量
     */
    public static final int API_MAX_LIMIT = 500;
}
