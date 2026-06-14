package com.blockchain;

import com.blockchain.param.SpotQueryParam;
import com.blockchain.service.SpotAnalysisService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author: 魏文昌
 * @description: 自然交易理论数据
 * @date: 2026/4/27
 */
@RunWith(SpringRunner.class)
@SpringBootTest
class NaturalTransactionTest {

    @Value("${api.key}")
    private String apiKey;

    @Value("${api.secretKey}")
    private String apiSecretKey;

    @Resource
    private SpotAnalysisService spotAnalysisService;

    @Test
    public void deleteDataTest(){
        spotAnalysisService.remove(null);
    }

    @Test
    public void buildDbDataTest(){
        // 4H
        SpotQueryParam btcSpotQueryParam = SpotQueryParam.builder()
                .instId("BTC-USDT")
                .bar("4H")
                .limit("120")
                .build();
        spotAnalysisService.parseData(btcSpotQueryParam);

        SpotQueryParam ethSpotQueryParam = SpotQueryParam.builder()
                .instId("ETH-USDT")
                .bar("4H")
                .limit("120")
                .build();
        spotAnalysisService.parseData(ethSpotQueryParam);

        //ThreadUtil.sleep(1000);
        // 2h
        spotAnalysisService.parseData(SpotQueryParam.builder()
                .instId("BTC-USDT")
                .bar("2H")
                .limit("80")
                .build());
        spotAnalysisService.parseData(SpotQueryParam.builder()
                .instId("ETH-USDT")
                .bar("2H")
                .limit("80")
                .build());

        //ThreadUtil.sleep(1000);
        //1h
        spotAnalysisService.parseData(SpotQueryParam.builder()
                .instId("BTC-USDT")
                .bar("1H")
                .limit("72")
                .build());
        spotAnalysisService.parseData(SpotQueryParam.builder()
                .instId("ETH-USDT")
                .bar("1H")
                .limit("72")
                .build());
    }


    @Test
    public void allDataTest(){
        //先删除数据再添加
        spotAnalysisService.remove(null);
        // 4H
        SpotQueryParam btcSpotQueryParam = SpotQueryParam.builder()
                .instId("BTC-USDT")
                .bar("4H")
                .limit("120")
                .build();
        spotAnalysisService.parseData(btcSpotQueryParam);

        SpotQueryParam ethSpotQueryParam = SpotQueryParam.builder()
                .instId("ETH-USDT")
                .bar("4H")
                .limit("120")
                .build();
        spotAnalysisService.parseData(ethSpotQueryParam);

        //ThreadUtil.sleep(1000);
        // 2h
        spotAnalysisService.parseData(SpotQueryParam.builder()
                .instId("BTC-USDT")
                .bar("2H")
                .limit("80")
                .build());
        spotAnalysisService.parseData(SpotQueryParam.builder()
                .instId("ETH-USDT")
                .bar("2H")
                .limit("80")
                .build());

        //ThreadUtil.sleep(1000);
        //1h
        spotAnalysisService.parseData(SpotQueryParam.builder()
                .instId("BTC-USDT")
                .bar("1H")
                .limit("72")
                .build());
        spotAnalysisService.parseData(SpotQueryParam.builder()
                .instId("ETH-USDT")
                .bar("1H")
                .limit("72")
                .build());
    }
}
