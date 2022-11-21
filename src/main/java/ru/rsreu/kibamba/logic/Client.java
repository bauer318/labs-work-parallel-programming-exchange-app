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

    public void withdraw(Currency currency, BigDecimal amount){
        synchronized (currency){
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
    }
    public void deposit(Currency currency, BigDecimal amount) {
        synchronized (currency) {
            BigDecimal currentBalance = balance.get(currency);
            BigDecimal newBalance = currentBalance.add(amount).setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE);
            balance.put(currency, newBalance);
        }
    }
    public Map<Currency, BigDecimal> getBalance() {
        return new HashMap<>(this.balance);
    }
    public int getId() {
        return id;
    }
}
