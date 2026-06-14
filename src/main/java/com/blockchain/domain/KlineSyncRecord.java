package com.blockchain.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * K线数据同步记录表
 * 用于记录每次拉取的同步状态，支持增量拉取
 *
 * @author blockchain
 */
@TableName(value = "kline_sync_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KlineSyncRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 交易对符号，如：BTCUSDT
     */
    private String symbol;

    /**
     * K线间隔，如：4h、1d、1w
     */
    private String timeInterval;

    /**
     * 数据存储类型：mysql/file
     */
    private String storageType;

    /**
     * 上次拉取的最后K线开盘时间（毫秒时间戳）
     */
    private Long lastOpenTime;

    /**
     * 上次拉取的最后K线收盘时间（毫秒时间戳）
     */
    private Long lastCloseTime;

    /**
     * 当前已拉取的K线数量
     */
    private Integer currentCount;

    /**
     * 目标K线数量
     */
    private Integer targetCount;

    /**
     * 最后同步时间
     */
    private LocalDateTime lastSyncTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}
