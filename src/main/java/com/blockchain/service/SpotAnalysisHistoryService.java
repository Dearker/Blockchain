package com.blockchain.service;

import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.extension.service.IService;
import com.blockchain.domain.SpotAnalysisHistory;
import com.blockchain.param.SpotQueryParam;

public interface SpotAnalysisHistoryService extends IService<SpotAnalysisHistory> {

    void parseData(SpotQueryParam spotQueryParam);

    /**
     * 写入数据到csv文件中
     *
     * @param spotQueryParam 查询条件
     */
    void dataToCsv(SpotQueryParam spotQueryParam);

    /**
     * 获取接口数据
     *
     * @param spotQueryParam 查询条件
     * @return 数据
     */
    JSONArray getResultData(SpotQueryParam spotQueryParam);
}