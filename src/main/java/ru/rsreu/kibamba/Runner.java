package ru.rsreu.kibamba;

import ru.rsreu.kibamba.model.*;
import ru.rsreu.kibamba.stockmarket.StockMarket;

import java.math.BigDecimal;

public class Runner {
    public static void main(String[] args) {
        Client client = new Client(1);
        client.deposit(Currency.RUB, new BigDecimal(1000.62));
        Order order = new Order(client, CurrencyPairs.USD_TO_RUB, OrderType.BUY, new BigDecimal(10), new BigDecimal(61.50));

        StockMarket stockMarket = new StockMarket();
        stockMarket.addOrder(order);

        stockMarket.getAllOrdersList().forEach(ord -> System.out.println(ord.getPrice()));
    }
}