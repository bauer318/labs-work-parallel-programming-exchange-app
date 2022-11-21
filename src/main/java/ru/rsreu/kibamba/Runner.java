package ru.rsreu.kibamba;

import ru.rsreu.kibamba.logic.*;
import ru.rsreu.kibamba.logic.Forex;

import java.math.BigDecimal;

public class Runner {
    public static void main(String[] args) {
        Client client = new Client(1);
        client.deposit(Currency.RUB, new BigDecimal(1000.62));
        Order order = new Order(client, CurrencyPairs.USD_RUB, OrderType.BUY, new BigDecimal(10), new BigDecimal(61.50));

        Forex forex = new Forex();
        forex.addOrder(order);

        forex.getAllOrdersList().forEach(ord -> System.out.println(ord.getPrice()));
    }
}