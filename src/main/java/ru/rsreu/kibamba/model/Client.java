package ru.rsreu.kibamba.model;

import ru.rsreu.kibamba.exception.NotEnoughMoneyException;
import ru.rsreu.kibamba.worker.CurrencyWorker;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Client {
    private final int id;
    private final Map<Currency, BigDecimal> balance;

    public Client(int id){
        this.id = id;
        Map<Currency, BigDecimal> balance = new ConcurrentHashMap<>(Currency.values().length);
        for (Currency currency : Currency.values()) {
            balance.put(currency, BigDecimal.ZERO.setScale(CurrencyWorker.CURRENCY_SCALE , CurrencyWorker.CURRENCY_ROUNDING_MODE));
        }
        this.balance = balance;
    }

    public void withdraw(Currency currency, BigDecimal amount){
        synchronized (currency){
            BigDecimal currentAmount = balance.get(currency);
            if (currentAmount.compareTo(amount.setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE)) == -1) {
                throw new NotEnoughMoneyException(String.format("Trying to withdraw %s %s, but the client %s has only %s",
                        amount.setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE), currency, this.getId(), currentAmount));
            }
            BigDecimal newAmount = currentAmount.subtract(amount).setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE);
            balance.put(currency, newAmount);
        }
    }
    public void deposit(Currency currency, BigDecimal amount) {
        synchronized (currency) {
            BigDecimal currentAmount = balance.get(currency);
            BigDecimal newAmount = currentAmount.add(amount).setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE);
            balance.put(currency, newAmount);
        }
    }
    public Map<Currency, BigDecimal> getBalance() {
        return new HashMap<>(this.balance);
    }
    public int getId() {
        return id;
    }
}
