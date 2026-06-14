package com.blockchain.service;

import com.blockchain.domain.SpotAnalysis;
import com.blockchain.domain.SpotAnalysisHistory;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class MacdCalculator {

    /**
     * 计算并填充列表中每一根 K 线的指标值
     * @param klines 原始 K 线列表
     * @return 填充了指标后的列表
     */
    public List<SpotAnalysis> calculateAndFill(List<SpotAnalysis> klines) {
        int size = klines.size();
        double[] closes = new double[size];
        for (int i = 0; i < size; i++) {
            closes[i] = klines.get(i).getPriceClose().doubleValue();
        }

        // 1. 计算全量 EMA
        double[] ema12 = calculateEMA(closes, 12);
        double[] ema26 = calculateEMA(closes, 26);

        // 2. 计算全量 DIF
        double[] difs = new double[size];
        for (int i = 0; i < size; i++) {
            difs[i] = ema12[i] - ema26[i];
        }

        // 3. 计算全量 DEA
        double[] deas = calculateEMA(difs, 9);

        // 4. 将结果回填至每一根 K 线实体中
        for (int i = 0; i < size; i++) {
            double currentDif = difs[i];
            double currentDea = deas[i];
            // TradingView 标准算法: MACD = DIF - DEA
            double currentMacd = currentDif - currentDea;

            SpotAnalysis item = klines.get(i);

            item.setDif(BigDecimal.valueOf(currentDif).setScale(6, RoundingMode.HALF_UP));
            item.setDea(BigDecimal.valueOf(currentDea).setScale(6, RoundingMode.HALF_UP));
            item.setMacd(BigDecimal.valueOf(currentMacd).setScale(6, RoundingMode.HALF_UP));
        }

        return klines;
    }

    /**
     * 计算并填充历史数据列表中每一根 K 线的指标值
     * @param klines 原始 K 线列表
     * @return 填充了指标后的列表
     */
    public List<SpotAnalysisHistory> calculateAndFillHistory(List<SpotAnalysisHistory> klines) {
        int size = klines.size();
        double[] closes = new double[size];
        for (int i = 0; i < size; i++) {
            closes[i] = klines.get(i).getPriceClose().doubleValue();
        }

        // 1. 计算全量 EMA
        double[] ema12 = calculateEMA(closes, 12);
        double[] ema26 = calculateEMA(closes, 26);

        // 2. 计算全量 DIF
        double[] difs = new double[size];
        for (int i = 0; i < size; i++) {
            difs[i] = ema12[i] - ema26[i];
        }

        // 3. 计算全量 DEA
        double[] deas = calculateEMA(difs, 9);

        // 4. 将结果回填至每一根 K 线实体中
        for (int i = 0; i < size; i++) {
            double currentDif = difs[i];
            double currentDea = deas[i];
            // TradingView 标准算法: MACD = DIF - DEA
            double currentMacd = currentDif - currentDea;

            SpotAnalysisHistory item = klines.get(i);

            item.setDif(BigDecimal.valueOf(currentDif).setScale(6, RoundingMode.HALF_UP));
            item.setDea(BigDecimal.valueOf(currentDea).setScale(6, RoundingMode.HALF_UP));
            item.setMacd(BigDecimal.valueOf(currentMacd).setScale(6, RoundingMode.HALF_UP));
        }

        return klines;
    }

    private double[] calculateEMA(double[] data, int period) {
        double[] ema = new double[data.length];
        double multiplier = 2.0 / (period + 1.0);
        ema[0] = data[0];
        for (int i = 1; i < data.length; i++) {
            ema[i] = (data[i] - ema[i - 1]) * multiplier + ema[i - 1];
        }
        return ema;
    }

}

