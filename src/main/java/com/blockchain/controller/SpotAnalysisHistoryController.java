package com.blockchain.controller;

import com.blockchain.domain.MacdResult;
import com.blockchain.domain.SpotAnalysisHistory;
import com.blockchain.param.SpotQueryParam;
import com.blockchain.service.BinanceService;
import com.blockchain.service.MacdCalculator;
import com.blockchain.service.SpotAnalysisHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/macd/history")
@RequiredArgsConstructor
public class SpotAnalysisHistoryController {

    private final BinanceService binanceService;

    private final MacdCalculator macdCalculator;

    private final SpotAnalysisHistoryService spotAnalysisHistoryService;

    /**
     * 获取 BTC 在不同时间级别的 MACD 数据（历史数据）
     * 访问地址: http://localhost:8080/api/macd/history/bnMacd
     */
    @PostMapping("/bnMacd")
    public Map<String, MacdResult> getBnMacd(SpotQueryParam spotQueryParam) {
        String symbol = "BTCUSDT";
        String[] intervals = {"1h", "4h", "1d", "3d"};
        Map<String, MacdResult> results = new LinkedHashMap<>();

        for (String interval : intervals) {
            // 1. 获取币安K线数据并解析为 SpotAnalysisHistory 实体列表
            List<SpotAnalysisHistory> spotAnalysisHistoryList = binanceService.getSpotAnalysisHistoryData(spotQueryParam);

            // 2. 传入实体列表，计算MACD指标
            if (spotAnalysisHistoryList != null && !spotAnalysisHistoryList.isEmpty()) {
                macdCalculator.calculateAndFillHistory(spotAnalysisHistoryList);
                //results.put(interval, result);
            }
        }
        return results;
    }

    @PostMapping("/okMacd")
    public void getOkMacd(SpotQueryParam spotQueryParam) {
        spotAnalysisHistoryService.parseData(spotQueryParam);
    }

}