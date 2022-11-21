package ru.rsreu.kibamba.logic;

import ru.rsreu.kibamba.exception.InsufficientBalance;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Client {
    private final int id;
    private final Map<Currency, BigDecimal> balance;

    public Client(int id){
        this.id = id;
        this.balance = initBalance();
    }
    private Map<Currency, BigDecimal> initBalance(){
        Map<Currency, BigDecimal> balance = new ConcurrentHashMap<>(Currency.values().length);
        for (Currency currency : Currency.values()) {
            balance.put(currency, new BigDecimal(BigDecimal.ZERO.doubleValue())
                    .setScale(CurrencyWorker.CURRENCY_SCALE,CurrencyWorker.CURRENCY_ROUNDING_MODE));
        }
        return balance;
    }
    synchronized public void deposit(Currency currency, BigDecimal amount) {
        BigDecimal currentBalance = balance.get(currency);
        BigDecimal newBalance = currentBalance.add(amount).setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE);
        balance.put(currency, newBalance);
    }

    synchronized public void withdraw(Currency currency, BigDecimal amount){
            BigDecimal currentBalance = balance.get(currency);
            if (currentBalance.compareTo(amount.setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE)) == -1) {
                throw new InsufficientBalance(String.format("Dear client %s your current balance %s is insufficient to make the withdrawal of %s currency %s ",
                        this.getId(),
                        currentBalance,
                        amount.setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE),
                        currency));
            }
            BigDecimal restBalance = currentBalance.subtract(amount).setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE);
            balance.put(currency, restBalance);
    }

    synchronized public void buy(CurrencyPairs currencyPairs, BigDecimal amount,BigDecimal price){
            BigDecimal leftCurrencyBalance = balance.get(currencyPairs.getLeftCurrency());
            BigDecimal newLeftCurrencyBalance = leftCurrencyBalance.subtract(amount)
                    .setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE);
            BigDecimal rightBalance = balance.get((currencyPairs.getRightCurrency()));
            BigDecimal amountPurchasedRightCurrency = amount.multiply(price)
                    .setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE);
            BigDecimal newRightCurrencyBalance = rightBalance.add(amountPurchasedRightCurrency)
                    .setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE);
            balance.put(currencyPairs.getRightCurrency(),newRightCurrencyBalance);
            balance.put(currencyPairs.getLeftCurrency(),newLeftCurrencyBalance);

    }
    synchronized public void sel(CurrencyPairs currencyPairs, BigDecimal amount, BigDecimal price){
            BigDecimal rightCurrencyBalance = balance.get(currencyPairs.getRightCurrency());
            BigDecimal newRightCurrencyBalance = rightCurrencyBalance.subtract(amount)
                    .setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE);
            BigDecimal leftCurrencyBalance = balance.get(currencyPairs.getLeftCurrency());
            BigDecimal amountSoldLeftCurrency = amount.multiply(price)
                    .setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE);
            BigDecimal newLeftCurrencyBalance = leftCurrencyBalance.add(amountSoldLeftCurrency)
                    .setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE);
            balance.put(currencyPairs.getRightCurrency(),newRightCurrencyBalance);
            balance.put(currencyPairs.getLeftCurrency(),newLeftCurrencyBalance);
    }

    public Map<Currency, BigDecimal> getBalance() {
        return new HashMap<>(this.balance);
    }
    public int getId() {
        return id;
    }
}
