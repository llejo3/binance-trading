package com.southman.trading;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.account.Account;
import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.account.NewOrder;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.Trade;
import com.binance.api.client.domain.account.request.OrderRequest;
import com.binance.api.client.domain.general.ExchangeInfo;
import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolFilter;
import com.binance.api.client.domain.general.SymbolInfo;
import com.binance.api.client.domain.market.TickerPrice;
import com.binance.api.client.exception.BinanceApiException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

/**
 * Examples on how to get market data information such as the latest price of a
 * symbol, etc.
 */
public class MarketOrderTrading extends TimerTask {
	
	static Logger logger = Logger.getLogger(MarketOrderTrading.class);
	
	private final String ITEMS_FILE_NAME = "trading_items.json";
	private final String CONFIG_FILE_NAME = "tradingBinance.properties";
	private final long CONFIG_REFLESH_DELAY_MILLISECONDS = 60000;
	private final Type ITEMS_TYPE = new TypeToken<Map<String, TradingItems>>() {}.getType();
	private final String NUMBER_FORMAT = "0.#########";
	
	private final String API_KEY = "API_KEY";
	private final String SECRET_KEY = "SECRET_KEY";
	
	private final PropertiesConfiguration config = new PropertiesConfiguration();
	private final FileChangedReloadingStrategy fileChangedReloadingStrategy =new FileChangedReloadingStrategy();

	private static String[] PAIR_SYMBOLS = null;
	private final Map<String, Double> CURRENCY_INVEST_PRICES = new HashMap<String, Double>();
	
	private double GAP_TRADING_PERCENT = 70.0;
	private boolean IS_RISK_TASKING = false;
	private double FEE_PERCENT = 0.05;
	private double TAKE_EXTRA_PERCENT = 2.0;
	private double MIN_START_PERCENT = 1.0;
	public static int TIME_PERIOD_MILLISECONDS = 500;
	private final Map<String, Double> MIN_ORDER_VALUES = new HashMap<String, Double>();
	private int STATUS_LOGGING_CYCLE = 600;
	private int SAVING_ITEMS_CYCLE = 60;

	private final BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(API_KEY, SECRET_KEY);
	private final BinanceApiRestClient client = factory.newRestClient();
	private final DecimalFormat numberFormatter = new DecimalFormat(NUMBER_FORMAT);
	

	private Map<String, TradingItems> items = null;
	private long exeCount = 0;
	

	public MarketOrderTrading() {
		loadConfig();
		initialSettings();
	}

	private void loadConfig() {
		config.setFileName(CONFIG_FILE_NAME);
		try {
			config.load();
			setConfig();
		} catch (ConfigurationException e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
		}
		
		fileChangedReloadingStrategy.setRefreshDelay(CONFIG_REFLESH_DELAY_MILLISECONDS);
		config.setReloadingStrategy(fileChangedReloadingStrategy);
	}
	
	private void setConfig() {
		PAIR_SYMBOLS = config.getStringArray("PAIR_SYMBOLS");
		
		CURRENCY_INVEST_PRICES.put("BTC", config.getDouble("CURRENCY_INVEST_PRICES.BTC", 0.002));
		CURRENCY_INVEST_PRICES.put("ETH", config.getDouble("CURRENCY_INVEST_PRICES.ETH", 0.02));
		CURRENCY_INVEST_PRICES.put("USDT", config.getDouble("CURRENCY_INVEST_PRICES.USDT", 11.0));
		CURRENCY_INVEST_PRICES.put("BNB", config.getDouble("CURRENCY_INVEST_PRICES.BNB", 2.0));
		
		GAP_TRADING_PERCENT  = config.getDouble("GAP_TRADING_PERCENT", 70.0);
		IS_RISK_TASKING = config.getBoolean("IS_RISK_TASKING", false);
		FEE_PERCENT = config.getDouble("FEE_PERCENT", 0.05);
		TAKE_EXTRA_PERCENT = config.getDouble("TAKE_EXTRA_PERCENT", 0.05);
		MIN_START_PERCENT = config.getDouble("MIN_START_PERCENT", 1.0);
		TIME_PERIOD_MILLISECONDS = config.getInt("TIME_PERIOD_MILLISECONDS", 500);
		
		MIN_ORDER_VALUES.put("BTC", config.getDouble("MIN_ORDER_VALUES.BTC", 0.001));
		MIN_ORDER_VALUES.put("ETH", config.getDouble("MIN_ORDER_VALUES.ETH", 0.01));
		MIN_ORDER_VALUES.put("USDT", config.getDouble("MIN_ORDER_VALUES.USDT", 10.0));
		MIN_ORDER_VALUES.put("BNB", config.getDouble("MIN_ORDER_VALUES.BNB", 1.0));

		STATUS_LOGGING_CYCLE = config.getInt("STATUS_LOGGING_CYCLE", 600);	
		SAVING_ITEMS_CYCLE = config.getInt("SAVING_ITEMS_CYCLE", 60);	
		
	}
	
	private void initialSettings() {
		if (items == null) {
			loadItems();
		}
		
		final Iterator<String> itemKeys = items.keySet().iterator();
		while (itemKeys.hasNext()) {
			final String key = itemKeys.next();
			if ( ArrayUtils.contains(PAIR_SYMBOLS, key) == false ) {
				itemKeys.remove();
			}
		}
		
		for (int i = 0; i < PAIR_SYMBOLS.length; i++) {
			final String pairSymbol = PAIR_SYMBOLS[i];
			if (items.containsKey(pairSymbol) == false) {
				initialBasicSetting(pairSymbol);
				initialSetting(items.get(pairSymbol));
			}
		}
	}
	
	private void initialBasicSetting(final String pairSymbol) {
		TradingItems item;
		if (items.containsKey(pairSymbol)) {
			item = this.items.get(pairSymbol);
		} else {
			item = new TradingItems();
			items.put(pairSymbol, item);
		}
		final String[] symbols = StringUtils.split(pairSymbol, '/');
		item.setTargetSymbol(symbols[0]);
		item.setCurrencySymbol(symbols[1]);
		item.setPairSymbol(symbols[0] + symbols[1]);
		getLotSize(item);
	}

	private void initialSetting(final TradingItems item) {
		final List<Order> openOrders = getOpenOrders(item.getPairSymbol());
		if (openOrders.size() > 0) {
			item.setWaiting(true);
			return;
		} else {
			item.setWaiting(false);
		}


		Trade trade = getLastTrade(item.getPairSymbol());
		double bagicPrice = 0.0;
		if (trade != null) {
			bagicPrice = Double.parseDouble(trade.getPrice());
			item.setLastTradePrice(bagicPrice);
		} else {
			final TickerPrice nowTickerPrice = getNowPrice(item.getPairSymbol());
			final double nowPrice = Double.parseDouble(nowTickerPrice.getPrice());
			bagicPrice = nowPrice;
		}

		item.setTopPrice(bagicPrice);
		item.setBottomPrice(bagicPrice);
		item.setLastOrderPrice(bagicPrice);

		logger.info(item.getPairSymbol() +  ":Bagic Price:" + toString(bagicPrice));
	}
	
	private void replaceSetting(final TradingItems item) {
		double bagicPrice;
		if (item.getLastTradePrice() > 0.0) {
			bagicPrice = item.getLastTradePrice();
		} else {
			Trade trade = getLastTrade(item.getPairSymbol());
			if (trade != null) {
				bagicPrice = Double.parseDouble(trade.getPrice());
				item.setLastTradePrice(bagicPrice);
			} else {
				bagicPrice = item.getNowPrice();
			}
		}
		
		item.setTopPrice(bagicPrice);
		item.setBottomPrice(bagicPrice);
		item.setLastOrderPrice(bagicPrice);
	}

	private void getLotSize(final TradingItems item) {
		final ExchangeInfo exchangeInfo = client.getExchangeInfo();
		final SymbolInfo symbolInfo = exchangeInfo.getSymbolInfo(item.getPairSymbol());
		final SymbolFilter priceFilter = symbolInfo.getSymbolFilter(FilterType.LOT_SIZE);
		item.setMinQty(Double.parseDouble(priceFilter.getMinQty()));
		item.setStepSize(Double.parseDouble(priceFilter.getStepSize()));
	}

	private TickerPrice getNowPrice(final String pairSymbol) {
		return client.getPrice(pairSymbol);
	}
	
	private void setNowPrice(final List<TickerPrice> tickerPrices, final TradingItems item) {
		final String pairSymbol = item.getPairSymbol();
		final int size = tickerPrices.size();
		for (int i=0; i<size; i++) {
			final TickerPrice tickerPrice = tickerPrices.get(i);
			if (StringUtils.equals(pairSymbol, tickerPrice.getSymbol())) {
				item.setNowPrice(Double.parseDouble(tickerPrice.getPrice()));
				break;
			}
		}
	}
	
	private List<TickerPrice> getAllPrices() {
		return client.getAllPrices();
	}
	
	private List<Order> getOpenOrders(final String pairSymbol) {
		return client.getOpenOrders(new OrderRequest(pairSymbol));
	}

	private Trade getLastTrade(final String pairSymbol) {
		Trade trade = null;
		List<Trade> trades = client.getMyTrades(pairSymbol, 1);
		if (trades.size() > 0) {
			trade = trades.get(0);
		}
		return trade;
	}

	private AssetBalance getAssetBalance(final String symbol) {
		final Account account = client.getAccount(6000000L, System.currentTimeMillis());
		return account.getAssetBalance(symbol);
	}

	private void watchTrading(final TradingItems item) {
		if (item.isWaiting()) {
			initialSetting(item);
			return;
		}
		final double lastOrderPrice = item.getLastOrderPrice();
		final double nowPrice = item.getNowPrice();
		
		if (nowPrice < lastOrderPrice) { // Buy
			item.setBuy(true);
			watchBuyTrading(item);
		} else { // Sell
			item.setBuy(false);
			watchSellTrading(item);
		}
	}

	private void watchBuyTrading(final TradingItems item) {
		final double bottomPrice = item.getBottomPrice();
		final double lastOrderPrice = item.getLastOrderPrice();
		final double nowPrice = item.getNowPrice();

		if (nowPrice < bottomPrice) {
			item.setBottomPrice(nowPrice);
		} else {

			if (this.IS_RISK_TASKING == false) {
				final double breakEvenPrice = lastOrderPrice
						- nowPrice * (TAKE_EXTRA_PERCENT + FEE_PERCENT) / 100.0;
				if (nowPrice > breakEvenPrice) {
					return;
				}
			}

			if (nowPrice - bottomPrice < nowPrice * (MIN_START_PERCENT / 100)) {
				return;
			}

			final double maxPrice = lastOrderPrice - (lastOrderPrice - bottomPrice) * (GAP_TRADING_PERCENT / 100.0);
			if (nowPrice < maxPrice) {
				return;
			}
			orderBuying(item, nowPrice);
		}
	}

	private void watchSellTrading(final TradingItems item) {
		final double topPrice = item.getTopPrice();
		final double lastOrderPrice = item.getLastOrderPrice();
		final double nowPrice = item.getNowPrice();
		
		if (nowPrice > topPrice) {
			item.setTopPrice(nowPrice);
		} else {

			if (this.IS_RISK_TASKING == false) {
				final double breakEvenPrice = lastOrderPrice
						+ nowPrice * (TAKE_EXTRA_PERCENT + FEE_PERCENT * 2) / 100.0;
				if (nowPrice < breakEvenPrice) {
					return;
				}
			}

			if (topPrice - nowPrice < nowPrice * (MIN_START_PERCENT / 100)) {
				return;
			}

			final double minPrice = lastOrderPrice + (topPrice - lastOrderPrice) * (GAP_TRADING_PERCENT / 100.0);
			if (nowPrice > minPrice) {
				return;
			}
			orderSelling(item, nowPrice);
			
		}
	}

	private String getPriceLogging(final TradingItems item) {
		return item.getPairSymbol() + ":" 
						+ (item.isBuy() ? "Buy" : "Sell") 
						+ ", N:" + toString(item.getNowPrice())
						+ ", T:" + toString(item.getTopPrice()) 
						+ ", B:"+ toString(item.getBottomPrice()) 
						+ ", L:" + toString(item.getLastOrderPrice());
	}
	
	private void sendStatusLogging() {
		if (exeCount % STATUS_LOGGING_CYCLE == 0) {
			final int simbolCnt = PAIR_SYMBOLS.length;
			final Random random = new Random();
			final int rIndex = random.nextInt(simbolCnt);
			final String pairSymbol = PAIR_SYMBOLS[rIndex];
			final TradingItems item = items.get(pairSymbol);
			logger.info("EXE CNT:" + exeCount + ",  " + getPriceLogging(item));
		}
	}

	private void orderBuying(final TradingItems item, final double price) {
		final String currencySymbol = item.getCurrencySymbol();
		final double minOrderValue = MIN_ORDER_VALUES.get(currencySymbol);
		final AssetBalance currencyBalance = getAssetBalance(currencySymbol);
		final double freeQty = Double.parseDouble(currencyBalance.getFree());
		final double currencyInvestPrice = CURRENCY_INVEST_PRICES.get(item.getCurrencySymbol());

		String buyQty = null;
		if (freeQty < minOrderValue) {
			logger.info(item.getPairSymbol() + ":Not enough currency.");
			replaceSetting(item);
			return;
		} else if (freeQty < currencyInvestPrice) {
			// BNB
			if (StringUtils.equals("BNB", currencySymbol)) {
				logger.info(item.getPairSymbol() + ":Not enough currency.");
				replaceSetting(item);
				return;
			}
			buyQty = toOrderQty(item, freeQty / price);
		} else {
			buyQty = toOrderQty(item, currencyInvestPrice / price);
		}

		try {
			client.newOrder(NewOrder.marketBuy(item.getPairSymbol(), buyQty));
			logger.info(item.getPairSymbol() + ":Buying:" + toString(price) + ", Qty:" + buyQty);
		} catch (BinanceApiException e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
		}
		initialSetting(item);
	}

	private void orderSelling(final TradingItems item, final double price) {
		final String currencySymbol = item.getCurrencySymbol();
		final String targetSymbol = item.getTargetSymbol();
		final double currencyInvestPrice = CURRENCY_INVEST_PRICES.get(currencySymbol);
		final double minOrderValue = MIN_ORDER_VALUES.get(currencySymbol);
		
		final AssetBalance balance = getAssetBalance(targetSymbol);
		final double freeQty = Double.parseDouble(balance.getFree());
		final double sellMaxQty =  currencyInvestPrice / price;
		final double sellMinQty =  minOrderValue / price;
		String sellQty = null;
		if (sellMinQty > freeQty) {
			logger.info(item.getPairSymbol() + ":Not enough target.");
			replaceSetting(item);
			return;
		} else if (sellMaxQty > freeQty) {
			// BNB
			if (StringUtils.equals("BNB", targetSymbol)) {
				logger.info(item.getPairSymbol() + ":Not enough target.");
				replaceSetting(item);
				return;
			}
			sellQty = toOrderQty(item, freeQty);
		} else {
			sellQty = toOrderQty(item, sellMaxQty);
		}
		
		try {
			client.newOrder(NewOrder.marketSell(item.getPairSymbol(), sellQty));
			logger.info(item.getPairSymbol() + ":Selling:" + toString(price) + ", Qty:" + sellQty);
		} catch (BinanceApiException e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
		}
		initialSetting(item);
	}

	private String toOrderQty(final TradingItems item, final double qty) {
		String strOrderQty = null;
		if (qty >= item.getMinQty()) {
			double orderQty = qty - qty % item.getStepSize();
			strOrderQty = toString(orderQty);
		}
		return strOrderQty;
	}

	private String toString(double num) {
		return numberFormatter.format(num);
	}

	private void saveItems() {
		if (exeCount % SAVING_ITEMS_CYCLE == 1) {
			final Gson gson = new Gson();
			final String jsonItems = gson.toJson(items);
			FileWriter fw = null;
			try {
				fw = new FileWriter(ITEMS_FILE_NAME);
				fw.write(jsonItems);
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
				e.printStackTrace();
			} finally {
				try {
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void loadItems() {
		JsonReader reader = null;
		Gson gson = new Gson();
		try {
			reader = new JsonReader(new FileReader(ITEMS_FILE_NAME));
			this.items = gson.fromJson(reader, ITEMS_TYPE);
		} catch (FileNotFoundException e) {
			items = new HashMap<String, TradingItems>();
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void run() {
		if (fileChangedReloadingStrategy.reloadingRequired()) {
			setConfig();
			initialSettings();
		}
		final List<TickerPrice> tickerPrices = getAllPrices(); 
		
		for (int i = 0; i < PAIR_SYMBOLS.length; i++) {
			final String pairSymbol = PAIR_SYMBOLS[i];
			final TradingItems item = items.get(pairSymbol);
			setNowPrice(tickerPrices, item);
			watchTrading(item);
		}
		sendStatusLogging();
		saveItems();
		
		exeCount++;
	}

	public static void main(String[] args) {
		try {
			final TimerTask task = new MarketOrderTrading();
			final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
			service.scheduleWithFixedDelay(task, TIME_PERIOD_MILLISECONDS, TIME_PERIOD_MILLISECONDS, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
		}
	}

}
