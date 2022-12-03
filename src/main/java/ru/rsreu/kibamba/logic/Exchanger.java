package ru.rsreu.kibamba.logic;

import ru.rsreu.kibamba.logic.Order.Order;
import ru.rsreu.kibamba.logic.Order.OrderStatus;
import ru.rsreu.kibamba.logic.currency.CurrencyPairs;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Exchanger {

    private final Map<CurrencyPairs, List<Order>> orders;

    public Exchanger() {
        this.orders = new ConcurrentHashMap<>(CurrencyPairs.values().length);
        for (CurrencyPairs currencyPair : CurrencyPairs.values()) {
            orders.put(currencyPair, new ArrayList<>());
        }
    }

    public void addOrder(Order incomingOrder) {
        synchronized (incomingOrder.getCurrencyPair()) {
            AtomicBoolean thereIsEqualOrder = new AtomicBoolean(false);
            AtomicReference<Order> foundTargetOrder = new AtomicReference<>(null);
            List<Order> targetOrders = orders.get(incomingOrder.getCurrencyPair());
            targetOrders.forEach(targetOrder -> {
                if (incomingOrder.compatibleWith(targetOrder)) {
                    closeOrders(incomingOrder, targetOrder);
                    foundTargetOrder.set(targetOrder);
                }
                if (!thereIsEqualOrder.get()) {
                    thereIsEqualOrder.set(incomingOrder.equals(targetOrder));
                }
            });
            if (incomingOrder.getOrderStatus() == OrderStatus.OPENED && !thereIsEqualOrder.get()) {
                orders.get(incomingOrder.getCurrencyPair()).add(incomingOrder);
            }
            if (foundTargetOrder.get() != null) {
                removeOrders(incomingOrder, foundTargetOrder.get());
            }
        }

    }

    public void closeOrders(Order incomingOrder, Order targetOrder) {
        BigDecimal dealPrice = targetOrder.getPrice();
        CurrencyPairs dealCurrencyPair = targetOrder.getCurrencyPair();
        BigDecimal dealAmount = targetOrder.getAmount();
        switch (targetOrder.getOrderType()) {
            case BUY: {
                targetOrder.getClient().buy(dealCurrencyPair,
                        dealAmount, dealPrice);
                incomingOrder.getClient().sel(dealCurrencyPair, dealAmount, dealPrice);
                break;
            }
            case SELL: {
                targetOrder.getClient().sel(dealCurrencyPair, dealAmount, dealPrice);
                incomingOrder.getClient().buy(dealCurrencyPair, dealAmount, dealPrice);
                break;
            }
        }
        incomingOrder.setOrderStatus(OrderStatus.CLOSED);
        targetOrder.setOrderStatus(OrderStatus.CLOSED);

    }

    private void removeOrders(Order incomingOrder, Order targetOrder) {
        orders.get(incomingOrder.getCurrencyPair()).remove(incomingOrder);
        orders.get(targetOrder.getCurrencyPair()).remove(targetOrder);
    }

    public List<Order> getOpenedOrders() {
        List<Order> allOrderList = new ArrayList<>();
        orders.values().forEach(allOrderList::addAll);
        return allOrderList;
    }

}
