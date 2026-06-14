-- K线数据表
CREATE TABLE IF NOT EXISTS `kline_data` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `symbol` VARCHAR(20) NOT NULL COMMENT '交易对符号，如：BTCUSDT',
    `interval` VARCHAR(10) NOT NULL COMMENT 'K线间隔，如：4h、1d、1w',
    `open_time` BIGINT NOT NULL COMMENT 'K线开盘时间（毫秒时间戳）',
    `open_price` DECIMAL(20, 8) NOT NULL COMMENT '开盘价',
    `high_price` DECIMAL(20, 8) NOT NULL COMMENT '最高价',
    `low_price` DECIMAL(20, 8) NOT NULL COMMENT '最低价',
    `close_price` DECIMAL(20, 8) NOT NULL COMMENT '收盘价',
    `volume` DECIMAL(30, 8) NOT NULL COMMENT '成交量',
    `close_time` BIGINT NOT NULL COMMENT 'K线收盘时间（毫秒时间戳）',
    `quote_asset_volume` DECIMAL(30, 8) DEFAULT NULL COMMENT '成交额',
    `number_of_trades` INT DEFAULT NULL COMMENT '成交笔数',
    `taker_buy_base_asset_volume` DECIMAL(30, 8) DEFAULT NULL COMMENT '主动买入成交量',
    `taker_buy_quote_asset_volume` DECIMAL(30, 8) DEFAULT NULL COMMENT '主动买入成交额',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_symbol_interval_opentime` (`symbol`, `interval`, `open_time`),
    KEY `idx_symbol_interval` (`symbol`, `interval`),
    KEY `idx_opentime` (`open_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='币安K线数据表';

-- K线同步记录表
CREATE TABLE IF NOT EXISTS `kline_sync_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `symbol` VARCHAR(20) NOT NULL COMMENT '交易对符号，如：BTCUSDT',
    `interval` VARCHAR(10) NOT NULL COMMENT 'K线间隔，如：4h、1d、1w',
    `storage_type` VARCHAR(10) NOT NULL COMMENT '数据存储类型：mysql/file',
    `last_open_time` BIGINT DEFAULT NULL COMMENT '上次拉取的最后K线开盘时间（毫秒时间戳）',
    `last_close_time` BIGINT DEFAULT NULL COMMENT '上次拉取的最后K线收盘时间（毫秒时间戳）',
    `current_count` INT DEFAULT 0 COMMENT '当前已拉取的K线数量',
    `target_count` INT NOT NULL COMMENT '目标K线数量',
    `last_sync_time` DATETIME DEFAULT NULL COMMENT '最后同步时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_symbol_interval_storagetype` (`symbol`, `interval`, `storage_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='K线数据同步记录表';
