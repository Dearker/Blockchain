package com.blockchain;

import cn.hutool.core.thread.ThreadUtil;
import com.blockchain.param.SpotQueryParam;
import com.blockchain.service.SpotAnalysisHistoryService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

/**
 * @author: 魏文昌
 * @description: 自然交易理论数据
 * @date: 2026/4/27
 */
@RunWith(SpringRunner.class)
@SpringBootTest
class NaturalTransactionHistoryTest {

    @Value("${api.key}")
    private String apiKey;

    @Value("${api.secretKey}")
    private String apiSecretKey;

    @Resource
    private SpotAnalysisHistoryService spotAnalysisHistoryService;

    /**
     * 删除所有数据
     */
    @Test
    public void deleteAllDataTest(){
        spotAnalysisHistoryService.remove(null);
    }

    /**
     * 构建实时数据库数据
     */
    @Test
    public void buildRealTimeDbDataTest(){
        // 4H
        SpotQueryParam btcSpotQueryParam = SpotQueryParam.builder()
                .instId("BTC-USDT")
                .bar("4H")
                .limit("120")
                .build();
        spotAnalysisHistoryService.parseData(btcSpotQueryParam);

        SpotQueryParam ethSpotQueryParam = SpotQueryParam.builder()
                .instId("ETH-USDT")
                .bar("4H")
                .limit("120")
                .build();
        spotAnalysisHistoryService.parseData(ethSpotQueryParam);

        //ThreadUtil.sleep(1000);
        // 2h
        spotAnalysisHistoryService.parseData(SpotQueryParam.builder()
                .instId("BTC-USDT")
                .bar("2H")
                .limit("80")
                .build());
        spotAnalysisHistoryService.parseData(SpotQueryParam.builder()
                .instId("ETH-USDT")
                .bar("2H")
                .limit("80")
                .build());

        //ThreadUtil.sleep(1000);
        //1h
        spotAnalysisHistoryService.parseData(SpotQueryParam.builder()
                .instId("BTC-USDT")
                .bar("1H")
                .limit("72")
                .build());
        spotAnalysisHistoryService.parseData(SpotQueryParam.builder()
                .instId("ETH-USDT")
                .bar("1H")
                .limit("72")
                .build());
    }

    /**
     * 构建所有实时数据
     */
    @Test
    public void allRealTimeDataTest(){
        //先删除数据再添加
        spotAnalysisHistoryService.remove(null);
        // 4H
        SpotQueryParam btcSpotQueryParam = SpotQueryParam.builder()
                .instId("BTC-USDT")
                .bar("4H")
                .limit("120")
                .build();
        spotAnalysisHistoryService.parseData(btcSpotQueryParam);

        SpotQueryParam ethSpotQueryParam = SpotQueryParam.builder()
                .instId("ETH-USDT")
                .bar("4H")
                .limit("120")
                .build();
        spotAnalysisHistoryService.parseData(ethSpotQueryParam);

        //ThreadUtil.sleep(1000);
        // 2h
        spotAnalysisHistoryService.parseData(SpotQueryParam.builder()
                .instId("BTC-USDT")
                .bar("2H")
                .limit("80")
                .build());
        spotAnalysisHistoryService.parseData(SpotQueryParam.builder()
                .instId("ETH-USDT")
                .bar("2H")
                .limit("80")
                .build());

        //ThreadUtil.sleep(1000);
        //1h
        spotAnalysisHistoryService.parseData(SpotQueryParam.builder()
                .instId("BTC-USDT")
                .bar("1H")
                .limit("72")
                .build());
        spotAnalysisHistoryService.parseData(SpotQueryParam.builder()
                .instId("ETH-USDT")
                .bar("1H")
                .limit("72")
                .build());
    }

    /**
     * 月K级别，拉取从当前时间开始之前一年的数据，总共12条数据，每批300条，每5次睡眠0.5s
     */
    @Test
    public void buildDbDataMonthTest(){
        // 一年约12个月，总共12条数据，1批即可拉取完
        long startData = LocalDateTime.of(LocalDate.now().minusYears(1), LocalTime.MIN)
                .toInstant(ZoneOffset.of("+8")).toEpochMilli();
        long endData = LocalDateTime.of(LocalDate.now(), LocalTime.MAX)
                .toInstant(ZoneOffset.of("+8")).toEpochMilli();

        SpotQueryParam spotQueryParam = SpotQueryParam.builder()
                .instId("BTC-USDT")
                .bar("1M")
                .limit("12")
                .before(String.valueOf(startData))
                .after(String.valueOf(endData))
                .build();
        spotAnalysisHistoryService.parseData(spotQueryParam);
    }

    /**
     * 周K级别，拉取从当前时间开始之前一年的数据，总共53条数据，每批300条，每5次睡眠0.5s
     */
    @Test
    public void buildDbDataWeekTest(){
        // 一年约53周，总共53条数据，1批即可拉取完
        long startData = LocalDateTime.of(LocalDate.now().minusYears(1), LocalTime.MIN)
                .toInstant(ZoneOffset.of("+8")).toEpochMilli();
        long endData = LocalDateTime.of(LocalDate.now(), LocalTime.MAX)
                .toInstant(ZoneOffset.of("+8")).toEpochMilli();

        SpotQueryParam spotQueryParam = SpotQueryParam.builder()
                .instId("BTC-USDT")
                .bar("1W")
                .limit("53")
                .before(String.valueOf(startData))
                .after(String.valueOf(endData))
                .build();
        spotAnalysisHistoryService.parseData(spotQueryParam);
    }

    /**
     * 日K级别，拉取从当前时间开始之前一年的数据，总共366条数据，每批300条，每5次睡眠0.5s
     */
    @Test
    public void buildDbDataDayTest(){
        // 一年366天，总共366条数据，需要2批拉取（每批300条）
        for (int i = 0; i < 2; i++) {
            int startDay = (i + 1) * 183;
            int endDay = i * 183;
            long startData = LocalDateTime.of(LocalDate.now().minusDays(startDay), LocalTime.MIN)
                    .toInstant(ZoneOffset.of("+8")).toEpochMilli();
            long endData = LocalDateTime.of(LocalDate.now().minusDays(endDay), LocalTime.MAX)
                    .toInstant(ZoneOffset.of("+8")).toEpochMilli();

            SpotQueryParam spotQueryParam = SpotQueryParam.builder()
                    .instId("BTC-USDT")
                    .bar("1D")
                    .limit("300")
                    .before(String.valueOf(startData))
                    .after(String.valueOf(endData))
                    .build();
            spotAnalysisHistoryService.parseData(spotQueryParam);
        }
    }

    /**
     * 4H级别，拉取从当前时间开始之前一年的数据，总共2196条数据，每批300条，每5次睡眠0.5s
     */
    @Test
    public void buildDbData4HTest(){
        // 一年2196条数据，每批300条，需要8批拉取
        for (int i = 0; i < 8; i++) {
            int startDay = (i + 1) * 50;
            int endDay = i * 50;
            long startData = LocalDateTime.of(LocalDate.now().minusDays(startDay), LocalTime.MIN)
                    .toInstant(ZoneOffset.of("+8")).toEpochMilli();
            long endData = LocalDateTime.of(LocalDate.now().minusDays(endDay), LocalTime.MAX)
                    .toInstant(ZoneOffset.of("+8")).toEpochMilli();

            SpotQueryParam spotQueryParam = SpotQueryParam.builder()
                    .instId("BTC-USDT")
                    .bar("4H")
                    .limit("300")
                    .before(String.valueOf(startData))
                    .after(String.valueOf(endData))
                    .build();
            spotAnalysisHistoryService.parseData(spotQueryParam);
            
            // 每5次睡眠0.5s
            if ((i + 1) % 5 == 0) {
                ThreadUtil.sleep(500);
            }
        }
    }

    /**
     * 2H级别，拉取从当前时间开始之前一年的数据，总共4392条数据，每批300条，每5次睡眠0.5s
     */
    @Test
    public void buildDbData2HTest(){
        // 一年4392条数据，每批300条，需要15批拉取
        for (int i = 0; i < 15; i++) {
            int startDay = (i + 1) * 25;
            int endDay = i * 25;
            long startData = LocalDateTime.of(LocalDate.now().minusDays(startDay), LocalTime.MIN)
                    .toInstant(ZoneOffset.of("+8")).toEpochMilli();
            long endData = LocalDateTime.of(LocalDate.now().minusDays(endDay), LocalTime.MAX)
                    .toInstant(ZoneOffset.of("+8")).toEpochMilli();

            SpotQueryParam spotQueryParam = SpotQueryParam.builder()
                    .instId("BTC-USDT")
                    .bar("2H")
                    .limit("300")
                    .before(String.valueOf(startData))
                    .after(String.valueOf(endData))
                    .build();
            spotAnalysisHistoryService.parseData(spotQueryParam);
            
            // 每5次睡眠0.5s
            if ((i + 1) % 5 == 0) {
                ThreadUtil.sleep(500);
            }
        }
    }

    /**
     * 1H级别，拉取从当前时间开始之前一年的数据，总共8784条数据，每批300条，每5次睡眠0.5s
     */
    @Test
    public void buildDbData1HTest(){
        // 一年8784条数据，每批300条，需要30批拉取
        for (int i = 0; i < 30; i++) {
            int startDay = (i + 1) * 13;
            int endDay = i * 13;
            long startData = LocalDateTime.of(LocalDate.now().minusDays(startDay), LocalTime.MIN)
                    .toInstant(ZoneOffset.of("+8")).toEpochMilli();
            long endData = LocalDateTime.of(LocalDate.now().minusDays(endDay), LocalTime.MAX)
                    .toInstant(ZoneOffset.of("+8")).toEpochMilli();

            SpotQueryParam spotQueryParam = SpotQueryParam.builder()
                    .instId("BTC-USDT")
                    .bar("1H")
                    .limit("300")
                    .before(String.valueOf(startData))
                    .after(String.valueOf(endData))
                    .build();
            spotAnalysisHistoryService.parseData(spotQueryParam);
            
            // 每5次睡眠0.5s
            if ((i + 1) % 5 == 0) {
                ThreadUtil.sleep(500);
            }
        }
    }


}
