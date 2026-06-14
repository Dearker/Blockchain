package com.blockchain.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 币安K线数据实体类
 *
 * @author blockchain
 */
@TableName(value = "kline_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KlineData implements Serializable {

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
     * K线开盘时间（毫秒时间戳）
     */
    private Long openTime;

    /**
     * 开盘价
     */
    private BigDecimal openPrice;

    /**
     * 最高价
     */
    private BigDecimal highPrice;

    /**
     * 最低价
     */
    private BigDecimal lowPrice;

    /**
     * 收盘价
     */
    private BigDecimal closePrice;

    /**
     * 成交量
     */
    private BigDecimal volume;

    /**
     * K线收盘时间（毫秒时间戳）
     */
    private Long closeTime;

    /**
     * 成交额
     */
    private BigDecimal quoteAssetVolume;

    /**
     * 成交笔数
     */
    private Integer numberOfTrades;

    /**
     * 主动买入成交量
     */
    private BigDecimal takerBuyBaseAssetVolume;

    /**
     * 主动买入成交额
     */
    private BigDecimal takerBuyQuoteAssetVolume;

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
