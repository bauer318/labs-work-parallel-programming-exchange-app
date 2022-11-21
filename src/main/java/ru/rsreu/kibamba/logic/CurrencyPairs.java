package ru.rsreu.kibamba.logic;

public enum CurrencyPairs {
    USD_CAD(Currency.USD, Currency.CAD),
    EUR_USD(Currency.EUR, Currency.USD),
    USD_CHF(Currency.USD, Currency.CHF),
    GBP_USD(Currency.GBP,Currency.USD),
    NZD_USD(Currency.NZD,Currency.USD),
    EUR_JPY(Currency.EUR,Currency.JPY),
    EUR_CHF(Currency.EUR,Currency.CHF),
    AUD_CAD(Currency.AUD,Currency.CAD),
    GBP_CHF(Currency.GBP,Currency.CHF),
    RUB_CNY(Currency.RUB, Currency.CNY),
    USD_RUB(Currency.USD,Currency.RUB),
    EUR_RUB(Currency.EUR,Currency.RUB),
    USD_EUR(Currency.USD,Currency.EUR);
    private final Currency leftCurrency;
    private final Currency rightCurrency;

    CurrencyPairs(Currency leftCurrency, Currency rightCurrency){
        this.leftCurrency = leftCurrency;
        this.rightCurrency = rightCurrency;
    }

    public Currency getLeftCurrency() {
        return leftCurrency;
    }

    public Currency getRightCurrency() {
        return rightCurrency;
    }
}
