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
        Client client = new Client(1);
        client.deposit(Currency.RUB, new BigDecimal(1000.62));
        Order order = new Order(client, CurrencyPairs.USD_RUB, OrderType.BUY, new BigDecimal(10), new BigDecimal(61.50));

        Exchanger exchanger = new Exchanger();
        exchanger.addOrder(order);

        exchanger.getAllOrdersList().forEach(ord -> System.out.println(ord.getPrice()));
    }
}