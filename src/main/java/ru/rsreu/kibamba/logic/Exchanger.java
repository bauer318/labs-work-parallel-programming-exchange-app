package ru.rsreu.kibamba.logic;

import ru.rsreu.kibamba.exception.IncompatibleOrder;
import ru.rsreu.kibamba.logic.Order.Order;
import ru.rsreu.kibamba.logic.currency.CurrencyPairs;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Exchanger {

    private final Map<CurrencyPairs, List<Order>> orders;

    public Exchanger() {
        this.orders = new ConcurrentHashMap<>(CurrencyPairs.values().length);
        for (CurrencyPairs currencyPair : CurrencyPairs.values()) {
            orders.put(currencyPair, new ArrayList<>());
        }
    }

    public void addOrder(Order order) {
        synchronized (order.getCurrencyPair()) {
            List<Order> clientOrders = orders.get(order.getCurrencyPair())
                    .stream().collect(Collectors.toList());
            clientOrders.forEach(orderCandidate -> {
                BigDecimal dealAmount = orderCandidate.getAmount().min(order.getAmount());
                BigDecimal dealPrice = getDealPrice(order, orderCandidate);

                if (reduceOrder(orderCandidate, dealAmount, dealPrice)) {
                    closeOrder(orderCandidate);
                }
                if (reduceOrder(order, dealAmount, dealPrice)) {
                    order.revoke();
                }
            });

            if (order.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                orders.get(order.getCurrencyPair()).add(order);
            }
        }
    }

    public List<Order> getAllOrdersList() {
        List<Order> allOrderList = new ArrayList<>();
        orders.values().forEach(allOrderList::addAll);
        return allOrderList;
    }

    public void revokeAllOrders() {
        getAllOrdersList().forEach(Order::revoke);
        orders.values().forEach(List::clear);
    }

    private boolean filterByClient(Order orderTarget, Order orderCandidate) {
        return orderCandidate.getClient().getId() != orderTarget.getClient().getId();
    }

    private boolean filterByType(Order orderTarget, Order orderCandidate) {
        return !orderCandidate.getOrderType().equals(orderTarget.getOrderType());
    }

    private boolean filterByPrice(Order orderTarget, Order orderCandidate) {
        switch (orderTarget.getOrderType()) {
            case BUY: {
                return orderTarget.getPrice().compareTo(orderCandidate.getPrice()) >= 0;
            }
            case SELL: {
                return orderTarget.getPrice().compareTo(orderCandidate.getPrice()) <= 0;
            }
            default: {
                throw new IncompatibleOrder();
            }
        }
    }

    private int compareOrdersForSorting(Order order1, Order order2, Order orderTarget) {
        switch (orderTarget.getOrderType()) {
            case BUY: {
                return order1.getPrice().compareTo(order2.getPrice());
            }
            case SELL: {
                return (-1) * order1.getPrice().compareTo(order2.getPrice());
            }
            default: {
                throw new IncompatibleOrder();
            }
        }
    }

    private BigDecimal getDealPrice(Order orderTarget, Order orderCandidate) {
        switch (orderTarget.getOrderType()) {
            case BUY: {
                return orderTarget.getPrice().min(orderCandidate.getPrice());
            }
            case SELL: {
                return orderTarget.getPrice().max(orderCandidate.getPrice());
            }
            default: {
                throw new IncompatibleOrder();
            }
        }
    }

    private boolean reduceOrder(Order order, BigDecimal amount, BigDecimal price) {
        order.reduce(amount, price);
        return order.getAmount().compareTo(BigDecimal.ZERO) == 0;
    }

    private void closeOrder(Order order) {
        order.revoke();
        orders.get(order.getCurrencyPair()).remove(order);
    }

}
