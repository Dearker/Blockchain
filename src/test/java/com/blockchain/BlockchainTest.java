package com.blockchain;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import com.blockchain.param.SpotQueryParam;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public class BlockchainTest {

    @Test
    public void okTest(){
        String ok = "https://www.okx.com/";
        String url =  ok + "/api/v5/market/history-candles?instId=BTC-USDT";

        String api = "8b90f30e-91b9-468b-9212-b173daddc7e2";
        String apiKey = "C1C41DF4B84595ED57608013FF23B352";

        System.out.println(DateUtil.date(1597026383085L));
    }

    @Test
    public void beanMapTest(){
        SpotQueryParam spotQueryParam = new SpotQueryParam();
        System.out.println(BeanUtil.beanToMap(spotQueryParam));
        System.out.println(System.currentTimeMillis());
    }

    @Test
    public void dataTest() {
        long epochSecond = LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.MAX)
                .toInstant(ZoneOffset.of("+8")).toEpochMilli();
        System.out.println(epochSecond);
        System.out.println(DateUtil.date(epochSecond));

        long second = LocalDateTime.of(LocalDate.now().minusDays(101), LocalTime.MIN)
                .toInstant(ZoneOffset.of("+8")).toEpochMilli();
        System.out.println(second);
        System.out.println(DateUtil.date(second));

        System.out.println(ChronoUnit.DAYS.between(LocalDateTime.of(LocalDate.now().minusDays(101), LocalTime.MIN),
                LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.MAX)));
    }


}
