package OrderTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import ru.rsreu.kibamba.exception.InsufficientBalance;
import ru.rsreu.kibamba.logic.Order.Order;
import ru.rsreu.kibamba.logic.Order.OrderStatus;
import ru.rsreu.kibamba.logic.Order.OrderType;
import ru.rsreu.kibamba.logic.client.Client;
import ru.rsreu.kibamba.logic.currency.Currency;
import ru.rsreu.kibamba.logic.currency.CurrencyPairs;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class TestOrder {

    @Test
    public void testOpenedBuyOrder(){
        Client buyer = new Client(1);
        buyer.deposit(Currency.CAD,new BigDecimal(27));
        BigDecimal amountUSD = new BigDecimal(20);
        BigDecimal priceUSDToCAD = new BigDecimal("1.35");
        Order orderBuyUSDWithAUD =
                new Order(buyer, CurrencyPairs.USD_CAD, OrderType.BUY,amountUSD,priceUSDToCAD);
        Assertions.assertEquals(OrderStatus.OPENED,orderBuyUSDWithAUD.getOrderStatus());
    }
    @Test
    public void testOpenedSelOrder(){
        Client seller = new Client(1);
        seller.deposit(Currency.AUD,new BigDecimal(350));
        seller.deposit(Currency.USD,new BigDecimal(19));
        BigDecimal amountToSold = new BigDecimal(19);
        BigDecimal priceUSDToAUD = new BigDecimal("1.51");
        Order orderSelUSDGetAUD =
                new Order(seller,CurrencyPairs.USD_AUD,OrderType.SELL,amountToSold,priceUSDToAUD);
        Assertions.assertEquals(OrderStatus.OPENED, orderSelUSDGetAUD.getOrderStatus());
    }

    @Test
    public void testInsufficientBalanceExceptionToSendOrder(){
        Client client = new Client(1);
        client.deposit(Currency.USD,new BigDecimal(78));
        client.withdraw(Currency.USD,new BigDecimal(8));
        Assertions.assertThrows(InsufficientBalance.class,()->{new Order(client,
                    CurrencyPairs.CHF_USD,OrderType.BUY,new BigDecimal("72.9"),new BigDecimal("1.04"));
        });

    }

    @RepeatedTest(100)
    public void testSelBuyRequestOrderWithoutChangeBalanceMultithreading(){
        CountDownLatch latch = new CountDownLatch(1);
        Client client = new Client(1);
        client.deposit(Currency.JPY,new BigDecimal(80));
        client.deposit(Currency.EUR,new BigDecimal("0.57"));
        AtomicReference<Order> orderBuyEURWithJPY = new AtomicReference<>();
        AtomicReference<Order> orderSelEURGetJPY = new AtomicReference<>();
        Runnable depositJPYTask = ()->{
            try{
                latch.await();
                client.deposit(Currency.JPY,new BigDecimal(350));
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        };
        Runnable orderBuyTask = () ->{
            try{
               latch.await();
                orderBuyEURWithJPY.set(new Order(client,CurrencyPairs.EUR_JPY,
                        OrderType.BUY,new BigDecimal("0.55"),new BigDecimal("144.74")));
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        };
        Runnable orderSelTask = ()->{
            try{
                latch.await();
                orderSelEURGetJPY.set(new Order(client,CurrencyPairs.EUR_JPY,OrderType.SELL,
                        new BigDecimal("0.55"),new BigDecimal("145.48")));
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        };
        List<Thread> threads = new ArrayList<>();
        for(int i=0; i<25;i++){
            threads.add(new Thread(depositJPYTask));
            threads.add(new Thread(orderBuyTask));
            threads.add(new Thread(orderSelTask));
        }
        threads.forEach(Thread::start);
        latch.countDown();
        threads.forEach(thread -> {
            try{
                thread.join();
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        });
        Assertions.assertEquals(0,
                client.getAssets().get(Currency.JPY).compareTo(new BigDecimal(8830)));
        Assertions.assertEquals(0,
                client.getAssets().get(Currency.EUR).compareTo((new BigDecimal("0.57"))));
    }
    @Test
    public void testTwoCompatibleOrders(){
        Client seller = new Client(1);
        seller.deposit(Currency.USD,new BigDecimal(50));
        Client buyer = new Client(2);
        buyer.deposit(Currency.RUB,new BigDecimal(3125));
        Order orderBuyUSDWithRUB = new Order(buyer,CurrencyPairs.USD_RUB,OrderType.BUY,
                new BigDecimal(50),new BigDecimal(62));
        Order orderSelUSDGetRUB = new Order(seller,CurrencyPairs.USD_RUB,
                OrderType.SELL,new BigDecimal(50),new BigDecimal("62.5"));
        Assertions.assertTrue(orderBuyUSDWithRUB.compatibleWith(orderSelUSDGetRUB));
    }
    @Test
    public void testTwoIncompatibleOrdersByClient(){
        Client client = new Client(1);
        client.deposit(Currency.USD,new BigDecimal(50));
        client.deposit(Currency.RUB,new BigDecimal(3125));
        Order orderBuyUSDWithRUB = new Order(client,CurrencyPairs.USD_RUB,OrderType.BUY,
                new BigDecimal(50),new BigDecimal(62));
        Order orderSelUSDGetRUB = new Order(client,CurrencyPairs.USD_RUB,
                OrderType.SELL,new BigDecimal(50),new BigDecimal("62.5"));
        Assertions.assertFalse(orderBuyUSDWithRUB.compatibleWith(orderSelUSDGetRUB));
    }
    @Test
    public void testTwoIncompatibleOrdersByOrderType(){
        Client firstBuyer = new Client(1);
        firstBuyer.deposit(Currency.RUB,new BigDecimal(5000));
        Client secondBuyer = new Client(2);
        secondBuyer.deposit(Currency.RUB,new BigDecimal(7500));
        Order firstOrderBuy = new Order(firstBuyer,CurrencyPairs.USD_RUB,OrderType.BUY,
                new BigDecimal(50),new BigDecimal(62));
        Order secondOrderBuy = new Order(secondBuyer,CurrencyPairs.USD_RUB,
                OrderType.BUY,new BigDecimal(50),new BigDecimal("62.5"));
        Assertions.assertFalse(firstOrderBuy.compatibleWith(secondOrderBuy));
    }
    @Test
    public void testTwoIncompatibleOrdersByCurrencyPair(){
        Client buyer = new Client(1);
        buyer.deposit(Currency.RUB,new BigDecimal(3300));
        Client seller = new Client(2);
        seller.deposit(Currency.EUR,new BigDecimal(50));
        Order orderBuyRUBWithUSD = new Order(buyer,CurrencyPairs.USD_RUB,OrderType.BUY,
                new BigDecimal(50),new BigDecimal(62));
        Order orderSelRUBGetUSD = new Order(seller,CurrencyPairs.EUR_RUB,
                OrderType.SELL,new BigDecimal(50),new BigDecimal("62.5"));
        Assertions.assertFalse(orderBuyRUBWithUSD.compatibleWith(orderSelRUBGetUSD));
    }

    @Test
    public void testTwoIncompatibleOrdersByTargetOrderPrice(){
        Client seller = new Client(1);
        seller.deposit(Currency.USD,new BigDecimal(50));
        Client buyer = new Client(2);
        buyer.deposit(Currency.RUB,new BigDecimal(3100));
        Order orderBuyUSDWithRUB = new Order(buyer,CurrencyPairs.USD_RUB,OrderType.BUY,
                new BigDecimal(50),new BigDecimal(62));
        Order orderSelUSDGetRUB = new Order(seller,CurrencyPairs.USD_RUB,
                OrderType.SELL,new BigDecimal(50),new BigDecimal("62.5"));
        Assertions.assertFalse(orderBuyUSDWithRUB.compatibleWith(orderSelUSDGetRUB));
    }
}
