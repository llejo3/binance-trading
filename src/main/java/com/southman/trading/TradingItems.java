package com.southman.trading;

public class TradingItems {

	private double lastOrderPrice = 0.0;
	private double topPrice = 0.0;
	private double bottomPrice = 0.0;
	private double nowPrice = 0.0;
	private double lastTradePrice = 0.0;
	
	private boolean isBuy = true;
	private boolean isWaiting = false;
	private String currencySymbol;
	private String targetSymbol;
	private String pairSymbol;
	
	private double minPrice = 0.0;
	private double tickSize = 0.0;
	private double minQty = 0.0;
	private double stepSize = 0.0;
	
	public double getLastOrderPrice() {
		return lastOrderPrice;
	}
	public void setLastOrderPrice(double lastOrderPrice) {
		this.lastOrderPrice = lastOrderPrice;
	}
	public double getTopPrice() {
		return topPrice;
	}
	public void setTopPrice(double topPrice) {
		this.topPrice = topPrice;
	}
	public double getBottomPrice() {
		return bottomPrice;
	}
	public void setBottomPrice(double bottomPrice) {
		this.bottomPrice = bottomPrice;
	}
	public boolean isBuy() {
		return isBuy;
	}
	public void setBuy(boolean isBuy) {
		this.isBuy = isBuy;
	}
	public double getMinQty() {
		return minQty;
	}
	public void setMinQty(double minQty) {
		this.minQty = minQty;
	}
	public double getStepSize() {
		return stepSize;
	}
	public void setStepSize(double stepSize) {
		this.stepSize = stepSize;
	}
	public String getPairSymbol() {
		return pairSymbol;
	}
	public void setPairSymbol(String pairSymbol) {
		this.pairSymbol = pairSymbol;
	}
	public String getTargetSymbol() {
		return targetSymbol;
	}
	public void setTargetSymbol(String targetSymbol) {
		this.targetSymbol = targetSymbol;
	}
	public boolean isWaiting() {
		return isWaiting;
	}
	public void setWaiting(boolean isWaiting) {
		this.isWaiting = isWaiting;
	}
	/**
	 * @return the minPrice
	 */
	public double getMinPrice() {
		return minPrice;
	}
	/**
	 * @param minPrice the minPrice to set
	 */
	public void setMinPrice(double minPrice) {
		this.minPrice = minPrice;
	}
	/**
	 * @return the tickSize
	 */
	public double getTickSize() {
		return tickSize;
	}
	/**
	 * @param tickSize the tickSize to set
	 */
	public void setTickSize(double tickSize) {
		this.tickSize = tickSize;
	}
	public double getNowPrice() {
		return nowPrice;
	}
	public void setNowPrice(double nowPrice) {
		this.nowPrice = nowPrice;
	}
	public String getCurrencySymbol() {
		return currencySymbol;
	}
	public void setCurrencySymbol(String currencySymbol) {
		this.currencySymbol = currencySymbol;
	}
	public double getLastTradePrice() {
		return lastTradePrice;
	}
	public void setLastTradePrice(double lastTradePrice) {
		this.lastTradePrice = lastTradePrice;
	}

	
}
