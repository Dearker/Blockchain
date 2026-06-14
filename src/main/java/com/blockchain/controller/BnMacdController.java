package com.blockchain.controller;

import com.blockchain.domain.MacdResult;
import com.blockchain.domain.SpotAnalysis;
import com.blockchain.param.SpotQueryParam;
import com.blockchain.service.BinanceService;
import com.blockchain.service.MacdCalculator;
import com.blockchain.service.SpotAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/macd")
@RequiredArgsConstructor
public class BnMacdController {

    private final BinanceService binanceService;

    private final MacdCalculator macdCalculator;

    private final SpotAnalysisService spotAnalysisService;

    /**
     * 获取 BTC 在不同时间级别的 MACD 数据
     * 访问地址: http://localhost:8080/api/macd/btc
     */
    @PostMapping("/bnMacd")
    public Map<String, MacdResult> getBnMacd(SpotQueryParam spotQueryParam) {
        String symbol = "BTCUSDT";
        String[] intervals = {"1h", "4h", "1d", "3d"};
        Map<String, MacdResult> results = new LinkedHashMap<>();

        for (String interval : intervals) {
            // 1. 获取币安K线数据并解析为 SpotAnalysis 实体列表
            List<SpotAnalysis> spotAnalysisList = binanceService.getSpotAnalysisData(spotQueryParam);

            // 2. 传入实体列表，计算MACD指标
            if (spotAnalysisList != null && !spotAnalysisList.isEmpty()) {
                macdCalculator.calculateAndFill(spotAnalysisList);
                //results.put(interval, result);
            }
        }
        return results;
    }

    @PostMapping("/okMacd")
    public void getOkMacd(SpotQueryParam spotQueryParam) {
        spotAnalysisService.parseData(spotQueryParam);
    }

}
