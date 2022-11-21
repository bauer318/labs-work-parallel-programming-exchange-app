package ru.rsreu.kibamba.model;

public enum CurrencyPairs {
    USD_TO_CAD(Currency.USD, Currency.CAD),
    EUR_TO_USD(Currency.EUR, Currency.USD),
    USD_TO_CHF(Currency.USD, Currency.CHF),
    GBP_TO_USD(Currency.GBP,Currency.USD),
    NZD_TO_USD(Currency.NZD,Currency.USD),
    EUR_TO_JPY(Currency.EUR,Currency.JPY),
    EUR_TO_CHF(Currency.EUR,Currency.CHF),
    AUD_TO_CAD(Currency.AUD,Currency.CAD),
    GBP_TO_CHF(Currency.GBP,Currency.CHF),
    RUB_TO_CNY(Currency.RUB, Currency.CNY),
    USD_TO_RUB(Currency.USD,Currency.RUB),
    EUR_TO_RUB(Currency.EUR,Currency.RUB),
    USD_TO_EUR(Currency.USD,Currency.EUR);
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
