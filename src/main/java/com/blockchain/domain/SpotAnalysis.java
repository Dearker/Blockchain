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
 * @TableName spot_analysis
 */
@TableName(value ="spot_analysis")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpotAnalysis implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 币种名称
     */
    private String moneyType;

    /**
     * 时间类型,[1s/1m/3m/5m/15m/30m/1H/2H/4H]
     */
    private String timeType;

    /**
     * 最低价
     */
    private BigDecimal priceLow;

    /**
     * 最高价
     */
    private BigDecimal priceHigh;

    /**
     * 价差
     */
    private BigDecimal priceDifferent;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 最低价时间
     */
    private LocalDateTime lowTime;

    /**
     * 最高时间
     */
    private LocalDateTime highTime;

    /**
     * 开盘价
     */
    private BigDecimal priceOpen;

    /**
     * 收盘价
     */
    private BigDecimal priceClose;

    /**
     * 振幅
     */
    private BigDecimal amplitude;

    /**
     * 涨跌幅
     */
    private BigDecimal priceLimit;

    /**
     * 交易量，如：btc总数
     */
    private BigDecimal volume;

    /**
     * 交易量，USDT总数
     */
    private BigDecimal volumeCcy;

    /**
     * K线状态
     * 0：K线未完结
     * 1：K线已完结
     */
    private Integer confirm;

    /**
     * MACD
     */
    private BigDecimal macd;

    /**
     * DIF
     */
    private BigDecimal dif;

    /**
     * DEA
     */
    private BigDecimal dea;

}