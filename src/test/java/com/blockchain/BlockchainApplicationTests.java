package com.blockchain;

import cn.hutool.core.thread.ThreadUtil;
import com.blockchain.domain.SpotAnalysis;
import com.blockchain.listener.BotMessageListener;
import com.blockchain.param.SpotQueryParam;
import com.blockchain.service.OkxMacdService;
import com.blockchain.service.SpotAnalysisService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;

//@RunWith(SpringRunner.class)
@SpringBootTest
class BlockchainApplicationTests {

	@Value("${api.key}")
	private String apiKey;

	@Value("${api.secretKey}")
	private String apiSecretKey;

	@Resource
	private SpotAnalysisService spotAnalysisService;

	//@Resource
	private BotMessageListener botMessageListener;

	@Resource
	private OkxMacdService okxMacdService;

	@Test
	void contextLoads() {
		SpotAnalysis spotAnalysis = new SpotAnalysis();
		spotAnalysis.setPriceLow(new BigDecimal("12.32"));
		spotAnalysis.setPriceHigh(new BigDecimal("12.32"));
		spotAnalysisService.save(spotAnalysis);
	}

	@Test
	public void botTest(){
		botMessageListener.getFirstMessage();
	}

	@Test
	public void buildDayDataTest(){
		List<String> instIdList = List.of("BTC-USDT", "ETH-USDT", "SOL-USDT", "BNB-USDT");

		LinkedHashMap<Integer, Integer> map = LinkedHashMap.newLinkedHashMap(8);
		map.put(101, 1);
		map.put(202, 102);
		map.put(303, 203);
		map.put(404,304);
		map.put(505,405);
		map.put(606,506);
		map.put(707,607);

		instIdList.forEach(s -> {
			map.forEach((k,v) -> {
				long startData = LocalDateTime.of(LocalDate.now().minusDays(k), LocalTime.MIN)
						.toInstant(ZoneOffset.of("+8")).toEpochMilli();
				long endData = LocalDateTime.of(LocalDate.now().minusDays(v), LocalTime.MAX)
						.toInstant(ZoneOffset.of("+8")).toEpochMilli();

				SpotQueryParam spotQueryParam = SpotQueryParam.builder()
						.instId(s)
						.bar("1D")
						.before(String.valueOf(startData))
						.after(String.valueOf(endData))
						.build();
				spotAnalysisService.parseData(spotQueryParam);
			});
			ThreadUtil.sleep(1000);
		});
	}

	@Test
	public void buildWeekDataTest(){
		List<String> instIdList = List.of("BTC-USDT", "ETH-USDT", "SOL-USDT", "BNB-USDT");

		instIdList.forEach(s -> {
			SpotQueryParam spotQueryParam = SpotQueryParam.builder()
					.instId(s)
					.bar("1W")
					.build();
			spotAnalysisService.parseData(spotQueryParam);
		});
	}

	@Test
	public void buildMonthDataTest(){
		List<String> instIdList = List.of("BTC-USDT", "ETH-USDT", "SOL-USDT", "BNB-USDT");

		instIdList.forEach(s -> {
			SpotQueryParam spotQueryParam = SpotQueryParam.builder()
					.instId(s)
					.limit("24")
					.bar("1M")
					.build();
			spotAnalysisService.parseData(spotQueryParam);
		});
	}

	/**
	 * 查询数据写入到csv中
	 */
	@Test
	public void buildData15mTest(){
		long startData = LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.MIN)
				.toInstant(ZoneOffset.of("+8")).toEpochMilli();
		long endData = LocalDateTime.of(LocalDate.now(), LocalTime.MAX)
				.toInstant(ZoneOffset.of("+8")).toEpochMilli();

		// [1m/3m/5m/15m/30m/1H/2H/4H]
		SpotQueryParam spotQueryParam = SpotQueryParam.builder()
				.instId("ETH-USDT")
				.bar("15m")
				.before(String.valueOf(startData))
				.after(String.valueOf(endData))
				.build();
		spotAnalysisService.dataToCsv(spotQueryParam);
	}

	@Test
	public void buildData1HTest(){
		long startData = LocalDateTime.of(LocalDate.now().minusDays(4), LocalTime.MIN)
				.toInstant(ZoneOffset.of("+8")).toEpochMilli();
		long endData = LocalDateTime.of(LocalDate.now(), LocalTime.MAX)
				.toInstant(ZoneOffset.of("+8")).toEpochMilli();

		// [1m/3m/5m/15m/30m/1H/2H/4H]
		SpotQueryParam spotQueryParam = SpotQueryParam.builder()
				.instId("ETH-USDT")
				.bar("1H")
				.before(String.valueOf(startData))
				.after(String.valueOf(endData))
				.build();
		spotAnalysisService.dataToCsv(spotQueryParam);
	}

	@Test
	public void buildData4HTest(){
		long startData = LocalDateTime.of(LocalDate.now().minusDays(15), LocalTime.MIN)
				.toInstant(ZoneOffset.of("+8")).toEpochMilli();
		long endData = LocalDateTime.of(LocalDate.now(), LocalTime.MAX)
				.toInstant(ZoneOffset.of("+8")).toEpochMilli();

		// [1m/3m/5m/15m/30m/1H/2H/4H]
		SpotQueryParam spotQueryParam = SpotQueryParam.builder()
				.instId("ETH-USDT")
				.bar("4H")
				.before(String.valueOf(startData))
				.after(String.valueOf(endData))
				.build();
		spotAnalysisService.dataToCsv(spotQueryParam);
	}

	@Test
	public void buildDbData1HTest(){
		for (int i = 0; i < 3; i++) {
			int startDay = (i + 1) * 13;
			int endDay = i * 13;
			long startData = LocalDateTime.of(LocalDate.now().minusDays(startDay), LocalTime.MIN)
					.toInstant(ZoneOffset.of("+8")).toEpochMilli();
			long endData = LocalDateTime.of(LocalDate.now().minusDays(endDay), LocalTime.MAX)
					.toInstant(ZoneOffset.of("+8")).toEpochMilli();

			// [1m/3m/5m/15m/30m/1H/2H/4H]
			SpotQueryParam spotQueryParam = SpotQueryParam.builder()
					.instId("BTC-USDT")
					.bar("1H")
					.limit("300")
					.before(String.valueOf(startData))
					.after(String.valueOf(endData))
					.build();
			spotAnalysisService.parseData(spotQueryParam);
			ThreadUtil.sleep(500);
		}
	}

	@Test
	public void buildDbData4HTest(){
		long startData = LocalDateTime.of(LocalDate.now().minusDays(50), LocalTime.MIN)
				.toInstant(ZoneOffset.of("+8")).toEpochMilli();
		long endData = LocalDateTime.of(LocalDate.now(), LocalTime.MAX)
				.toInstant(ZoneOffset.of("+8")).toEpochMilli();

		// [1m/3m/5m/15m/30m/1H/2H/4H]
		SpotQueryParam spotQueryParam = SpotQueryParam.builder()
				.instId("BTC-USDT")
				.bar("4H")
				.limit("300")
				.before(String.valueOf(startData))
				.after(String.valueOf(endData))
				.build();
		spotAnalysisService.parseData(spotQueryParam);
	}

	@Test
	public void buildTa4jDbData1HTest(){
		long startData = LocalDateTime.of(LocalDate.now().minusDays(42), LocalTime.MIN)
				.toInstant(ZoneOffset.of("+8")).toEpochMilli();
		long endData = LocalDateTime.of(LocalDate.now(), LocalTime.MAX)
				.toInstant(ZoneOffset.of("+8")).toEpochMilli();

		// [1m/3m/5m/15m/30m/1H/2H/4H]
		SpotQueryParam spotQueryParam = SpotQueryParam.builder()
				.instId("BTC-USDT")
				.bar("1H")
				.limit("1000")
				.before(String.valueOf(startData))
				.after(String.valueOf(endData))
				.build();
		okxMacdService.getAnalysisWithMacd(spotQueryParam);
	}

}
