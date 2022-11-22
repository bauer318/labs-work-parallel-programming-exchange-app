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
        buyer.deposit(Currency.USD,new BigDecimal(250));
        BigDecimal amountAUDCurrency = new BigDecimal(165);
        BigDecimal priceUSDToCAD = new BigDecimal("0.66");
        Order orderBuyAUDWithUSD =
                new Order(buyer, CurrencyPairs.USD_AUD, OrderType.BUY,amountAUDCurrency,priceUSDToCAD);
        Assertions.assertEquals(OrderStatus.OPENED,orderBuyAUDWithUSD.getOrderStatus());
        /*Assertions.assertEquals(0,buyer.getBalance().get(Currency.USD).compareTo(new BigDecimal(250)));
        Assertions.assertEquals(0,buyer.getBalance().get(Currency.AUD).compareTo(BigDecimal.ZERO));*/
    }
    @Test
    public void testOpenedSelOrder(){
        Client seller = new Client(1);
        seller.deposit(Currency.AUD,new BigDecimal(350));
        seller.deposit(Currency.USD,new BigDecimal(19));
        BigDecimal amountToSold = new BigDecimal("19");
        BigDecimal priceUSDToAUD = new BigDecimal("1.51");
        Order orderSelUSDGetAUD =
                new Order(seller,CurrencyPairs.USD_AUD,OrderType.SELL,amountToSold,priceUSDToAUD);
        Assertions.assertEquals(OrderStatus.OPENED, orderSelUSDGetAUD.getOrderStatus());
    }

    @Test
    public void testInsufficientBalanceExceptionToSendOrder(){
        Client client = new Client(1);
        client.deposit(Currency.CHF,new BigDecimal(78));
        client.withdraw(Currency.CHF,new BigDecimal(8));
        Assertions.assertThrows(InsufficientBalance.class,()->{
            Order order = new Order(client,
                    CurrencyPairs.CHF_USD,OrderType.BUY,new BigDecimal("72.9"),new BigDecimal("1.04"));
        });

    }

    @RepeatedTest(35)
    public void testSelBuyRequestOrderWithoutChangeBalanceMultithreading(){
        CountDownLatch latch = new CountDownLatch(1);
        Client client = new Client(1);
        client.deposit(Currency.EUR,new BigDecimal(80));
        AtomicReference<Order> orderBuyJPYWithEUR = new AtomicReference<>();
        AtomicReference<Order> orderSelEURGetJPY = new AtomicReference<>();
        Runnable depositUSDTask = ()->{
            try{
                latch.await();
                client.deposit(Currency.EUR,new BigDecimal(350));
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        };
        Runnable orderBuyTask = () ->{
            try{
               latch.await();
                orderBuyJPYWithEUR.set(new Order(client,CurrencyPairs.EUR_JPY,
                        OrderType.BUY,new BigDecimal(10_000),new BigDecimal("145.48")));
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        };
        Runnable orderSelTask = ()->{
            try{
                latch.await();
                orderSelEURGetJPY.set(new Order(client,CurrencyPairs.EUR_JPY,OrderType.SELL,
                        new BigDecimal(20),new BigDecimal("145.48")));
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        };
        List<Thread> threads = new ArrayList<>();
        for(int i=0; i<25;i++){
            threads.add(new Thread(depositUSDTask));
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
                client.getBalance().get(Currency.EUR).compareTo(new BigDecimal(8830)));
        Assertions.assertEquals(0,
                client.getBalance().get(Currency.JPY).compareTo((BigDecimal.ZERO)));
    }
    @Test
    public void testTwoCompatibleOrders(){
        Client buyer = new Client(1);
        buyer.deposit(Currency.USD,new BigDecimal(50));
        Client seller = new Client(2);
        seller.deposit(Currency.RUB,new BigDecimal(3300));
        Order orderBuyRUBWithUSD = new Order(buyer,CurrencyPairs.USD_RUB,OrderType.BUY,
                new BigDecimal(3100),new BigDecimal(62));
        Order orderSelRUBGetUSD = new Order(seller,CurrencyPairs.USD_RUB,
                OrderType.SELL,new BigDecimal(3200),new BigDecimal("0.01"));
        Assertions.assertTrue(orderBuyRUBWithUSD.compatibleWith(orderSelRUBGetUSD));
    }
    @Test
    public void testTwoIncompatibleOrdersByClient(){
        Client buyer = new Client(1);
        buyer.deposit(Currency.USD,new BigDecimal(50));
        buyer.deposit(Currency.RUB,new BigDecimal(3300));
        Order orderBuyRUBWithUSD = new Order(buyer,CurrencyPairs.USD_RUB,OrderType.BUY,
                new BigDecimal(3100),new BigDecimal(62));
        Order orderSelRUBGetUSD = new Order(buyer,CurrencyPairs.USD_RUB,
                OrderType.SELL,new BigDecimal(3200),new BigDecimal("0.01"));
        Assertions.assertFalse(orderBuyRUBWithUSD.compatibleWith(orderSelRUBGetUSD));
    }
    @Test
    public void testTwoIncompatibleOrdersByOrderType(){
        Client buyer = new Client(1);
        buyer.deposit(Currency.USD,new BigDecimal(50));
        Client secondBuyer = new Client(2);
        secondBuyer.deposit(Currency.USD,new BigDecimal(100));
        Order orderBuyRUBWithUSD = new Order(buyer,CurrencyPairs.USD_RUB,OrderType.BUY,
                new BigDecimal(3100),new BigDecimal(62));
        Order orderSelRUBGetUSD = new Order(secondBuyer,CurrencyPairs.USD_RUB,
                OrderType.BUY,new BigDecimal(5000),new BigDecimal("60"));
        Assertions.assertFalse(orderBuyRUBWithUSD.compatibleWith(orderSelRUBGetUSD));
    }
    @Test
    public void testTwoIncompatibleOrdersByCurrencyPair(){
        Client buyer = new Client(1);
        buyer.deposit(Currency.USD,new BigDecimal(50));
        Client seller = new Client(2);
        seller.deposit(Currency.RUB,new BigDecimal(3300));
        Order orderBuyRUBWithUSD = new Order(buyer,CurrencyPairs.USD_RUB,OrderType.BUY,
                new BigDecimal(3100),new BigDecimal(62));
        Order orderSelRUBGetUSD = new Order(seller,CurrencyPairs.EUR_RUB,
                OrderType.SELL,new BigDecimal(3200),new BigDecimal("0.01"));
        Assertions.assertFalse(orderBuyRUBWithUSD.compatibleWith(orderSelRUBGetUSD));
    }
}
