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
    private final Currency fromCurrency;
    private final Currency toCurrency;

    CurrencyPairs(Currency fromCurrency, Currency toCurrency){
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
    }

    public Currency getFromCurrency() {
        return fromCurrency;
    }

    public Currency getToCurrency() {
        return toCurrency;
    }
}
