package ru.rsreu.kibamba.logic.client;

import ru.rsreu.kibamba.exception.InsufficientBalance;
import ru.rsreu.kibamba.logic.currency.Currency;
import ru.rsreu.kibamba.logic.currency.CurrencyPairs;
import ru.rsreu.kibamba.logic.currency.CurrencyWorker;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Client {
    private final int id;
    private final Map<Currency, BigDecimal> assets;

    public Client(int id){
        this.id = id;
        this.assets = initAssets();
    }
    private Map<Currency, BigDecimal> initAssets(){
        Map<Currency, BigDecimal> assets = new ConcurrentHashMap<>(Currency.values().length);
        for (Currency currency : Currency.values()) {
            assets.put(currency, BigDecimal.valueOf(BigDecimal.ZERO.doubleValue())
                    .setScale(CurrencyWorker.CURRENCY_SCALE,CurrencyWorker.CURRENCY_ROUNDING_MODE));
        }
        return assets;
    }
    synchronized public void deposit(Currency currency, BigDecimal amount) {
        BigDecimal currentBalance = assets.get(currency);
        BigDecimal newBalance = currentBalance.add(amount)
                .setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE);
        assets.put(currency, newBalance);
    }

    synchronized public void withdraw(Currency currency, BigDecimal amount){
            BigDecimal currentBalance = assets.get(currency);
            if (currentBalance
                    .compareTo(amount.setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE)) < 0) {
                throw new InsufficientBalance(
                        String.format("Dear client %s your current balance %s is insufficient to make the withdrawal of %s currency %s ",
                        this.getId(),
                        currentBalance,
                        amount.setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE),
                        currency));
            }
            BigDecimal restBalance = currentBalance.subtract(amount).setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE);
            assets.put(currency, restBalance);
    }

    synchronized public BigDecimal getMaxLeftCurrencyToBuy(CurrencyPairs currencyPairs, BigDecimal price){
        return assets.get(currencyPairs.getRightCurrency())
                .divide(price,CurrencyWorker.CURRENCY_SCALE,CurrencyWorker.CURRENCY_ROUNDING_MODE)
                .setScale(CurrencyWorker.CURRENCY_SCALE,CurrencyWorker.CURRENCY_ROUNDING_MODE);
    }
    synchronized public void buy(CurrencyPairs currencyPairs, BigDecimal amountLeftCurrency, BigDecimal priceLeftToRightCurrency){
            BigDecimal leftCurrencyBalance = assets.get(currencyPairs.getLeftCurrency());
            BigDecimal rightBalance = assets.get((currencyPairs.getRightCurrency()));
            BigDecimal rightBalanceCurrencyToPaid = amountLeftCurrency.multiply(priceLeftToRightCurrency)
                    .setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE);
            BigDecimal newLeftCurrencyBalance = leftCurrencyBalance.add(amountLeftCurrency)
                    .setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE);
            BigDecimal newRightCurrencyBalance = rightBalance.subtract(rightBalanceCurrencyToPaid)
                    .setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE);
            assets.put(currencyPairs.getRightCurrency(),newRightCurrencyBalance);
            assets.put(currencyPairs.getLeftCurrency(),newLeftCurrencyBalance);

    }
    synchronized public void sel(CurrencyPairs currencyPairs, BigDecimal amountLeftCurrency, BigDecimal priceLeftToRightCurrency){
            BigDecimal leftCurrencyBalance = assets.get(currencyPairs.getLeftCurrency());
            BigDecimal rightCurrencyBalance = assets.get(currencyPairs.getRightCurrency());
            BigDecimal newLeftCurrencyBalance = leftCurrencyBalance.subtract(amountLeftCurrency)
                    .setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE);
            BigDecimal rightCurrencyAmountPurchased = amountLeftCurrency.multiply(priceLeftToRightCurrency)
                    .setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE);
            BigDecimal newRightCurrencyBalance = rightCurrencyBalance.add(rightCurrencyAmountPurchased)
                    .setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE);
            assets.put(currencyPairs.getRightCurrency(),newRightCurrencyBalance);
            assets.put(currencyPairs.getLeftCurrency(),newLeftCurrencyBalance);
    }

    public Map<Currency, BigDecimal> getAssets() {
        return new HashMap<>(this.assets);
    }
    public int getId() {
        return id;
    }
}
