package ru.rsreu.kibamba;

import ru.rsreu.kibamba.logic.Exchanger;
import ru.rsreu.kibamba.logic.Order.Order;
import ru.rsreu.kibamba.logic.Order.OrderType;
import ru.rsreu.kibamba.logic.client.Client;
import ru.rsreu.kibamba.logic.currency.Currency;
import ru.rsreu.kibamba.logic.currency.CurrencyPairs;

import java.math.BigDecimal;

public class Runner {
    public static void main(String[] args) {
       Exchanger exchanger = new Exchanger();
        Client seller = new Client(1);
        seller.deposit(Currency.USD,new BigDecimal(50));
        Client buyer = new Client(2);
        buyer.deposit(Currency.RUB,new BigDecimal(3125));
        Order orderBuyUSDWithRUB = new Order(buyer,CurrencyPairs.USD_RUB,OrderType.BUY,
                new BigDecimal(50),new BigDecimal(62));
        Order orderSelUSDGetRUB = new Order(seller,CurrencyPairs.USD_RUB,
                OrderType.SELL,new BigDecimal(50),new BigDecimal("62.5"));
        Order orderSelUSDGetRUB2 = new Order(seller,CurrencyPairs.USD_RUB,
                OrderType.SELL,new BigDecimal(50),new BigDecimal("63.5"));
        exchanger.addOrder(orderSelUSDGetRUB);
        exchanger.addOrder(orderSelUSDGetRUB2);
        exchanger.addOrder(orderBuyUSDWithRUB);
    }
}