package ru.rsreu.kibamba.logic.currency;

public enum CurrencyPairs {
    USD_CAD(Currency.USD, Currency.CAD),
    EUR_JPY(Currency.EUR,Currency.JPY),
    EUR_CHF(Currency.EUR,Currency.CHF),
    USD_RUB(Currency.USD,Currency.RUB),
    EUR_RUB(Currency.EUR,Currency.RUB),
    USD_AUD(Currency.USD,Currency.AUD),
    CHF_USD(Currency.CHF,Currency.USD);
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
