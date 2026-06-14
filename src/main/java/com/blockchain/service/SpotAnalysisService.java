package com.blockchain.service;

import com.alibaba.fastjson2.JSONArray;
import com.blockchain.domain.SpotAnalysis;
import com.baomidou.mybatisplus.extension.service.IService;
import com.blockchain.param.SpotQueryParam;

/**
* @author weiwenchang
* @description 针对表【spot_analysis(现货分析表)】的数据库操作Service
* @createDate 2024-09-11 21:54:00
*/
public interface SpotAnalysisService extends IService<SpotAnalysis> {

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
