package ExchangerTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import ru.rsreu.kibamba.logic.Exchanger;
import ru.rsreu.kibamba.logic.Order.Order;
import ru.rsreu.kibamba.logic.Order.OrderStatus;
import ru.rsreu.kibamba.logic.Order.OrderType;
import ru.rsreu.kibamba.logic.client.Client;
import ru.rsreu.kibamba.logic.currency.Currency;
import ru.rsreu.kibamba.logic.currency.CurrencyPairs;

import java.math.BigDecimal;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class TestExchanger {
    private void sleep(long timeMillis) {
        try {
            Thread.sleep(timeMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCloseOrdersWithSellerAsTargetOrder() {
        Exchanger exchanger = new Exchanger();
        Client seller = new Client(1);
        seller.deposit(Currency.USD, new BigDecimal(50));
        Client buyer = new Client(2);
        buyer.deposit(Currency.RUB, new BigDecimal(3125));
        Order orderBuyUSDWithRUB = new Order(buyer, CurrencyPairs.USD_RUB, OrderType.BUY,
                new BigDecimal(50), new BigDecimal(62));
        Order orderSelUSDGetRUB = new Order(seller, CurrencyPairs.USD_RUB,
                OrderType.SELL, new BigDecimal(50), new BigDecimal("62.5"));
        exchanger.closeOrders(orderBuyUSDWithRUB, orderSelUSDGetRUB);
        Assertions.assertEquals(OrderStatus.CLOSED, orderSelUSDGetRUB.getOrderStatus());
        Assertions.assertEquals(0, seller.getAssets().get(Currency.USD).compareTo(BigDecimal.ZERO));
        Assertions.assertEquals(0, seller.getAssets().get(Currency.RUB).compareTo(new BigDecimal(3125)));
        Assertions.assertEquals(0, buyer.getAssets().get(Currency.USD).compareTo(new BigDecimal(50)));
        Assertions.assertEquals(0, buyer.getAssets().get(Currency.RUB).compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testCloseOrdersWithBuyerAsTargetOrder() {
        Exchanger exchanger = new Exchanger();
        Client seller = new Client(1);
        seller.deposit(Currency.USD, new BigDecimal(50));
        Client buyer = new Client(2);
        buyer.deposit(Currency.RUB, new BigDecimal(3125));
        Order orderBuyUSDWithRUB = new Order(buyer, CurrencyPairs.USD_RUB, OrderType.BUY,
                new BigDecimal(50), new BigDecimal(62));
        Order orderSelUSDGetRUB = new Order(seller, CurrencyPairs.USD_RUB,
                OrderType.SELL, new BigDecimal(50), new BigDecimal("62.5"));
        exchanger.closeOrders(orderSelUSDGetRUB, orderBuyUSDWithRUB);
        Assertions.assertEquals(OrderStatus.CLOSED, orderBuyUSDWithRUB.getOrderStatus());
        Assertions.assertEquals(0, seller.getAssets().get(Currency.USD).compareTo(BigDecimal.ZERO));
        Assertions.assertEquals(0, seller.getAssets().get(Currency.RUB).compareTo(new BigDecimal(3100)));
        Assertions.assertEquals(0, buyer.getAssets().get(Currency.USD).compareTo(new BigDecimal(50)));
        Assertions.assertEquals(0, buyer.getAssets().get(Currency.RUB).compareTo(new BigDecimal(25)));
    }

    @RepeatedTest(50)
    public void testAddOrder() {
        Exchanger exchanger = new Exchanger();
        Client seller = new Client(1);
        seller.deposit(Currency.USD, new BigDecimal(50));
        Client buyer = new Client(2);
        buyer.deposit(Currency.RUB, new BigDecimal(3125));
        Order firstOrderBuyUSDWithRUB = new Order(buyer, CurrencyPairs.USD_RUB, OrderType.BUY,
                new BigDecimal(50), new BigDecimal(62));
        Order orderSelUSDGetRUB = new Order(seller, CurrencyPairs.USD_RUB,
                OrderType.SELL, new BigDecimal(50), new BigDecimal("62.5"));
        Order secondOrderBuyUSDWithRUB = new Order(buyer, CurrencyPairs.USD_RUB, OrderType.BUY,
                new BigDecimal(50), new BigDecimal(60));
        exchanger.addOrders(orderSelUSDGetRUB);
        exchanger.addOrders(orderSelUSDGetRUB);
        sleep(100);
        Assertions.assertEquals(1, exchanger.getOpenedOrders().size());
        exchanger.addOrders(firstOrderBuyUSDWithRUB);
        sleep(100);
        Assertions.assertEquals(0, exchanger.getOpenedOrders().size());
        firstOrderBuyUSDWithRUB.setOrderStatus(OrderStatus.OPENED);
        exchanger.addOrders(firstOrderBuyUSDWithRUB);
        exchanger.addOrders(secondOrderBuyUSDWithRUB);
        sleep(100);
        Assertions.assertEquals(2, exchanger.getOpenedOrders().size());
    }

    @RepeatedTest(100)
    public void testAddOrderWithTwoMatchingOrdersAsync() {
        Exchanger exchanger = new Exchanger();
        CountDownLatch latch = new CountDownLatch(1);
        Client seller = new Client(1);
        seller.deposit(Currency.EUR, new BigDecimal(250));
        Order orderSelEURGetCHF = new Order(seller, CurrencyPairs.EUR_CHF, OrderType.SELL, new BigDecimal(250), new BigDecimal("0.98"));

        Client buyer = new Client(2);
        buyer.deposit(Currency.CHF, new BigDecimal(247));
        Order orderBuyEURWithCHF = new Order(buyer, CurrencyPairs.EUR_CHF, OrderType.BUY, new BigDecimal(250), new BigDecimal("0.95"));

        Runnable addSelOrder = () -> {
            try {
                latch.await();
                exchanger.addOrders(orderSelEURGetCHF);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        Runnable addBuyOrder = () -> {
            try {
                latch.await();
                exchanger.addOrders(orderBuyEURWithCHF);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };

        Thread orderSelThread = new Thread(addSelOrder);
        Thread orderBuyThread = new Thread(addBuyOrder);

        orderSelThread.start();
        orderBuyThread.start();

        latch.countDown();
        try {
            orderSelThread.join();
            orderBuyThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sleep(200);
        Assertions.assertEquals(0, exchanger.getOpenedOrders().size());
        Assertions.assertEquals(0, seller.getAssets().get(Currency.EUR).compareTo(BigDecimal.ZERO));
        Assertions.assertTrue(buyer.getAssets().get(Currency.CHF).compareTo(new BigDecimal("2.0")) == 0 ||
                buyer.getAssets().get(Currency.CHF).compareTo(new BigDecimal("9.5")) == 0);
        Assertions.assertEquals(0, seller.getAssets().get(Currency.EUR).compareTo(BigDecimal.ZERO));
    }

    @RepeatedTest(100)
    public void testAddFourOrderAsync() {
        Exchanger exchanger = new Exchanger();
        CountDownLatch latch = new CountDownLatch(1);
        Client firstSeller = new Client(1);
        firstSeller.deposit(Currency.USD, new BigDecimal(450));
        Order firstSelOrder = new Order(firstSeller, CurrencyPairs.USD_RUB, OrderType.SELL, new BigDecimal(300), new BigDecimal(64));

        Client secondSeller = new Client(4);
        secondSeller.deposit(Currency.USD, new BigDecimal(315));
        Order secondSelOrder = new Order(secondSeller, CurrencyPairs.USD_RUB, OrderType.SELL, new BigDecimal(300), new BigDecimal(64));

        Client clientCanBuyAsTargetOrNot = new Client(2);
        clientCanBuyAsTargetOrNot.deposit(Currency.RUB, new BigDecimal(19200));
        Order firstOrderBuy = new Order(clientCanBuyAsTargetOrNot, CurrencyPairs.USD_RUB, OrderType.BUY, new BigDecimal(300), new BigDecimal(64));

        Client clientCanBuyOnlyAsTarget = new Client(3);
        clientCanBuyOnlyAsTarget.deposit(Currency.RUB, new BigDecimal(18600));
        Order secondBuyOrder = new Order(clientCanBuyOnlyAsTarget, CurrencyPairs.USD_RUB, OrderType.BUY, new BigDecimal(300), new BigDecimal(62));

        Runnable addFirstSelOrder = () -> {
            try {
                latch.await();
                exchanger.addOrders(firstSelOrder);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        Runnable addSecondSelOrder = () -> {
            try {
                latch.await();
                exchanger.addOrders(secondSelOrder);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        Runnable addFirstBuyOrder = () -> {
            try {
                latch.await();
                exchanger.addOrders(firstOrderBuy);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        Runnable addSecondBuyOrder = () -> {
            try {
                latch.await();
                exchanger.addOrders(secondBuyOrder);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };

        Thread firstSelOrderThread = new Thread(addFirstSelOrder);
        Thread secondSelOrderThread = new Thread(addSecondSelOrder);
        Thread firstBuyOrderThread = new Thread(addFirstBuyOrder);
        Thread secondBuyOrderThread = new Thread(addSecondBuyOrder);

        secondSelOrderThread.start();
        firstBuyOrderThread.start();
        secondBuyOrderThread.start();
        firstSelOrderThread.start();

        latch.countDown();
        try {
            firstSelOrderThread.join();
            secondSelOrderThread.join();
            firstBuyOrderThread.join();
            secondBuyOrderThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sleep(200);
        Assertions.assertTrue(exchanger.getOpenedOrders().size() == 2 || exchanger.getOpenedOrders().size() == 0);
    }

    @Test
    public void testOrdersTime() {
        Exchanger exchanger = new Exchanger();
        Client firstClient = new Client(1);
        Client secondClient = new Client(2);
        Client thirdClient = new Client(3);

        CountDownLatch latch = new CountDownLatch(1);

        firstClient.deposit(Currency.RUB, new BigDecimal(6000));
        secondClient.deposit(Currency.USD, new BigDecimal(100));
        thirdClient.deposit(Currency.EUR, new BigDecimal(100));

        CurrencyPairs[] currencyPairsArray = {CurrencyPairs.USD_RUB, CurrencyPairs.EUR_RUB};

        Runnable firstGroupOrdersTask = () -> {
            for (int i = 0; i <= 50; i++) {
                try{
                    latch.await();
                    Order buyUSDOrEURWithRUB = new Order(firstClient, currencyPairsArray[new Random().nextInt(2)],
                            OrderType.BUY, new BigDecimal(90), new BigDecimal(new Random().nextInt(5) + 60));
                    Order selUSDGetRUB = new Order(secondClient, CurrencyPairs.USD_RUB, OrderType.SELL, new BigDecimal(90),
                            new BigDecimal(new Random().nextInt(3) + 60));
                    Order selEURGetRUB = new Order(thirdClient, CurrencyPairs.EUR_RUB, OrderType.SELL, new BigDecimal(90),
                            new BigDecimal(new Random().nextInt(5) + 61));
                    exchanger.addOrders(buyUSDOrEURWithRUB,selEURGetRUB,selUSDGetRUB);
                    firstClient.deposit(Currency.RUB, new BigDecimal(7000));
                    secondClient.deposit(Currency.USD, new BigDecimal(150));
                    thirdClient.deposit(Currency.EUR, new BigDecimal(150));
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        };

        Runnable secondGroupOrdersTask = () ->{
            for (int i = 0; i <= 50; i++) {
                try{
                    latch.await();
                    firstClient.deposit(Currency.EUR, new BigDecimal(150));
                    firstClient.deposit(Currency.USD,new BigDecimal(150));
                    secondClient.deposit(Currency.RUB, new BigDecimal(7000));
                    thirdClient.deposit(Currency.RUB, new BigDecimal(7000));
                    Order buyUSDWithRUB = new Order(secondClient, CurrencyPairs.USD_RUB, OrderType.BUY, new BigDecimal(90), new BigDecimal(new Random().nextInt(3) + 58));
                    Order selUSDOrEURGetRUB = new Order(firstClient, currencyPairsArray[new Random().nextInt(2)], OrderType.SELL, new BigDecimal(90), new BigDecimal(new Random().nextInt(4) + 61));
                    Order buyEURWithRUB = new Order(thirdClient, CurrencyPairs.EUR_RUB, OrderType.BUY, new BigDecimal(90), new BigDecimal(new Random().nextInt(5) + 57));
                    exchanger.addOrders(buyUSDWithRUB,buyEURWithRUB,selUSDOrEURGetRUB);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        };

        Thread firstGroupOrdersThread = new Thread(firstGroupOrdersTask);
        Thread secondGroupOrdersThread = new Thread(secondGroupOrdersTask);
        long startTime = System.currentTimeMillis();
        firstGroupOrdersThread.start();
        secondGroupOrdersThread.start();
        latch.countDown();
        try{
            firstGroupOrdersThread.join();
            secondGroupOrdersThread.join();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        long endTime = System.currentTimeMillis();
        System.out.println(String.format("%s orders per sec",(3*300)/(endTime-startTime)/1000.0));
    }

}
