package com.blockchain.param;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpotQueryParam implements Serializable {

    /**
     * 产品ID，如 BTC-USDT
     */
    private String instId;

    /**
     * 请求此时间戳之后（更新的数据）的分页内容，传的值为对应接口的ts, 单独使用时，会返回最新的数据。
     */
    private String before;

    /**
     * 请求此时间戳之前（更旧的数据）的分页内容，传的值为对应接口的ts
     */
    private String after;

    /**
     * 时间粒度，默认值1m
     * 如 [1s/1m/3m/5m/15m/30m/1H/2H/4H]
     * 香港时间开盘价k线：[6H/12H/1D/2D/3D/1W/1M/3M]
     * UTC时间开盘价k线：[6Hutc/12Hutc/1Dutc/2Dutc/3Dutc/1Wutc/1Mutc/3Mutc]
     */
    private String bar;

    /**
     * 分页返回的结果集数量，最大为100，不填默认返回100条
     */
    private String limit;

}
