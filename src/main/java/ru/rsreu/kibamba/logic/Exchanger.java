package ru.rsreu.kibamba.logic;

import ru.rsreu.kibamba.logic.Order.Order;
import ru.rsreu.kibamba.logic.Order.OrderStatus;
import ru.rsreu.kibamba.logic.currency.CurrencyPairs;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Exchanger {

    private final Map<CurrencyPairs, List<Order>> orders;
    private final BlockingQueue<Order> orderBlockingQueue;
    private final Map<CurrencyPairs, BlockingQueue<Order>> orderBlockingQueueMap;

    public Exchanger() {
        this.orders = new ConcurrentHashMap<>(CurrencyPairs.values().length);
        this.orderBlockingQueueMap = new HashMap<>();
        for (CurrencyPairs currencyPair : CurrencyPairs.values()) {
            orders.put(currencyPair, new ArrayList<>());
            orderBlockingQueueMap.put(currencyPair,new LinkedBlockingQueue<>());
        }
        this.orderBlockingQueue = new LinkedBlockingQueue<>();
        consumeOrder();
    }

    private void produceOrder(Order incomingOrder) {
            orderBlockingQueue.add(incomingOrder);
    }
    public void addOrders(Order...orders){
        List<Thread> threads = new ArrayList<>();
        for(int i=0; i<orders.length;i++){
            int finalI = i;
            Thread t = new Thread(()->{
                produceOrder(orders[finalI]);
            });
            threads.add(t);
        }
        threads.forEach(Thread::start);
    }

    private void consumeOrder() {
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    Order order = orderBlockingQueue.take();
                    orderBlockingQueueMap.get(order.getCurrencyPair()).add(order);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.setDaemon(true);
        thread.start();

        Map<CurrencyPairs, Thread> currencyPairsThreadMap = new HashMap<>();
        for(Map.Entry<CurrencyPairs,BlockingQueue<Order>> entry:orderBlockingQueueMap.entrySet()){
            Thread t = new Thread(()->{
                while (true){
                    try{
                        Order order = entry.getValue().take();
                        processesOrders(order);
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            });
            t.setDaemon(true);
            currencyPairsThreadMap.put(entry.getKey(),t);
        }
        currencyPairsThreadMap.values().forEach(Thread::start);
    }

    private void processesOrders(Order incomingOrder){
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
        orderBlockingQueue.remove(incomingOrder);
        orderBlockingQueue.remove(targetOrder);
    }

    public List<Order> getOpenedOrders() {
        Set<Order> allOrderSet = new HashSet<>();
        allOrderSet.addAll(orderBlockingQueue);
        orders.values().forEach(allOrderSet::addAll);
        return new ArrayList<>(allOrderSet);
    }

}
