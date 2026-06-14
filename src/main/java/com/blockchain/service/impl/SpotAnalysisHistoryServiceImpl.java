package com.blockchain.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blockchain.dao.SpotAnalysisHistoryMapper;
import com.blockchain.domain.SpotAnalysisHistory;
import com.blockchain.param.SpotQueryParam;
import com.blockchain.service.MacdCalculator;
import com.blockchain.service.SpotAnalysisHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SpotAnalysisHistoryServiceImpl extends ServiceImpl<SpotAnalysisHistoryMapper, SpotAnalysisHistory>
        implements SpotAnalysisHistoryService {

    @Value("${api.key}")
    private String apiKey;

    @Value("${api.secretKey}")
    private String apiSecretKey;

    @Value("${api.passphrase}")
    private String apiPassphrase;

    private final CsvFileService csvFileService;

    private final MacdCalculator macdCalculator;

    /**
     * [
     * "1726149000000",
     * "57404",
     * "57413.9",
     * "57336.6",
     * "57375.3",
     * "37.96792152",
     * "2177832.154679836",
     * "2177832.154679836",
     * "1"
     * ]
     *
     * @param spotQueryParam 请求参数
     */
    public void parseData(SpotQueryParam spotQueryParam) {
        JSONArray jsonArray = this.getResultData(spotQueryParam);

        LocalDateTime localDateTime = LocalDateTime.now();
        List<SpotAnalysisHistory> spotAnalysisHistoryList = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONArray array = jsonArray.getJSONArray(i);
            BigDecimal priceHigh = array.getBigDecimal(2);
            BigDecimal priceLow = array.getBigDecimal(3);

            BigDecimal priceClose = array.getBigDecimal(4);
            BigDecimal priceOpen = array.getBigDecimal(1);

            //涨跌幅 =（收盘价-开盘价）/ 开盘价 * 100%
            BigDecimal priceLimit = NumberUtil.div(NumberUtil.sub(priceClose, priceOpen), priceOpen, 5)
                    .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
            //振幅 = (最高价 - 最低价) / 开盘价 * 100%
            BigDecimal amplitude = NumberUtil.div(NumberUtil.sub(priceHigh, priceLow), priceOpen, 5)
                    .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);

            SpotAnalysisHistory spotAnalysisHistory = SpotAnalysisHistory.builder()
                    .moneyType(spotQueryParam.getInstId())
                    .timeType(spotQueryParam.getBar())
                    .startTime(LocalDateTimeUtil.of(array.getLong(0)))
                    .priceOpen(priceOpen)
                    .priceHigh(priceHigh)
                    .priceLow(priceLow)
                    .priceClose(priceClose)
                    .volume(array.getBigDecimal(5))
                    .volumeCcy(array.getBigDecimal(6).setScale(2, RoundingMode.HALF_UP))
                    .confirm(array.getInteger(8))
                    .priceLimit(priceLimit)
                    .amplitude(amplitude)
                    .priceDifferent(NumberUtil.sub(priceHigh, priceLow))
                    .createTime(localDateTime)
                    .build();
            spotAnalysisHistoryList.add(spotAnalysisHistory);
            //System.out.println(spotAnalysisHistory);
        }
        System.out.println(spotAnalysisHistoryList.size());

        macdCalculator.calculateAndFillHistory(spotAnalysisHistoryList);
        super.saveBatch(spotAnalysisHistoryList);
    }

    @Override
    public JSONArray getResultData(SpotQueryParam spotQueryParam) {
        Map<String, Object> stringObjectMap = BeanUtil.beanToMap(spotQueryParam);
        List<String> paramList = new ArrayList<>();

        stringObjectMap.forEach((k, v) -> {
            if (Objects.nonNull(v)) {
                paramList.add(k + "=" + v);
            }
        });

        String paramString = String.join("&", paramList);

        String ok = "https://www.okx.com";
        //接口数据根据时间倒序返回
        String url = ok + "/api/v5/market/history-candles?" + paramString;

        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("apiKey", apiKey);
        paramMap.put("passphrase", apiPassphrase);
        paramMap.put("sign", apiSecretKey);
        paramMap.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));

        String body = HttpUtil.createGet(url).addHeaders(paramMap).execute().body();
        return JSON.parseObject(body).getJSONArray("data");
    }


    @Override
    public void dataToCsv(SpotQueryParam spotQueryParam) {
        JSONArray jsonArray = this.getResultData(spotQueryParam);

        String bar = spotQueryParam.getBar();
        List<String[]> dataList = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONArray array = jsonArray.getJSONArray(i);
            BigDecimal priceHigh = array.getBigDecimal(2);
            BigDecimal priceLow = array.getBigDecimal(3);

            BigDecimal priceClose = array.getBigDecimal(4);
            BigDecimal priceOpen = array.getBigDecimal(1);

            dataList.add(new String[]{
                    String.valueOf(LocalDateTimeUtil.of(array.getLong(0))),
                    priceOpen.toString(),
                    priceHigh.toString(),
                    priceLow.toString(),
                    priceClose.toString(),
                    String.valueOf(array.getBigDecimal(5)),
                    String.valueOf(array.getBigDecimal(6).setScale(2, RoundingMode.HALF_UP))});
        }
        csvFileService.generateSampleCsv(bar, dataList);
    }


}