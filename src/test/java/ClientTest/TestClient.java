package ClientTest;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import  org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import ru.rsreu.kibamba.exception.InsufficientBalance;
import ru.rsreu.kibamba.logic.client.Client;
import ru.rsreu.kibamba.logic.currency.Currency;
import ru.rsreu.kibamba.logic.currency.CurrencyPairs;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class TestClient {

    @Test
    public void testBuyUSDWithRUB(){
        Client buyer = new Client(1);
        BigDecimal amountDepositRUB = new BigDecimal(300);
        BigDecimal amountBuyUSD = new BigDecimal(3);
        BigDecimal priceUSDToRUB = new BigDecimal("62.35");
        buyer.deposit(Currency.RUB,amountDepositRUB);
        Assertions.assertEquals(0, buyer.getAssets().get(Currency.RUB).compareTo(amountDepositRUB));
        Assertions.assertEquals(0, buyer.getAssets().get(Currency.USD).compareTo(BigDecimal.ZERO));
        buyer.buy(CurrencyPairs.USD_RUB,amountBuyUSD,priceUSDToRUB);
        Assertions.assertEquals(0, buyer.getAssets().get(Currency.RUB).compareTo(new BigDecimal("112.95")));
        Assertions.assertEquals(0, buyer.getAssets().get(Currency.USD).compareTo(amountBuyUSD));
    }

    @Test
    public void testSellUSDGetRUB(){
        Client seller = new Client(1);
        BigDecimal amountDepositUSD = new BigDecimal(3);
        BigDecimal amountDepositRUB = new BigDecimal("112.95");
        BigDecimal amountSold = new BigDecimal(3);
        BigDecimal priceUSDToRUB = new BigDecimal(63);
        seller.deposit(Currency.USD,amountDepositUSD);
        seller.deposit(Currency.RUB,amountDepositRUB);
        Assertions.assertEquals(0, seller.getAssets().get(Currency.USD).compareTo(amountDepositUSD));
        Assertions.assertEquals(0, seller.getAssets().get(Currency.RUB).compareTo(amountDepositRUB));
        seller.sel(CurrencyPairs.USD_RUB,amountSold,priceUSDToRUB);
        Assertions.assertEquals(0, seller.getAssets().get(Currency.RUB).compareTo(new BigDecimal("301.95")));
        Assertions.assertEquals(0, seller.getAssets().get(Currency.USD).compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testMaxUSDToBuyWithAUD(){
        Client client = new Client(1);
        client.deposit(Currency.AUD, new BigDecimal(500));
        BigDecimal priceUSDToAUD = new BigDecimal("1.49");
        Assertions.assertEquals(new BigDecimal("335.57"),client.getMaxLeftCurrencyToBuy(CurrencyPairs.USD_AUD,priceUSDToAUD));

    }

    @Test
    public void testInsufficientBalanceException(){
        Client client = new Client(1);
        BigDecimal amountDepositUSD = new BigDecimal(300);
        BigDecimal amountWithdrawUSD = new BigDecimal("300.02");
        client.deposit(Currency.USD, amountDepositUSD);
        Assert.assertThrows(InsufficientBalance.class,()-> client.withdraw(Currency.USD,amountWithdrawUSD));
    }

    @RepeatedTest(100)
    public void testDepositWithdrawMultithreading(){
        Client client = new Client(1);
        BigDecimal amountDepositRUB = new BigDecimal(3000);
        client.deposit(Currency.RUB, amountDepositRUB);
        CountDownLatch latch = new CountDownLatch(1);

        Runnable depositMoneyTask = () -> {
            try {
                latch.await();
                client.deposit(Currency.RUB, new BigDecimal(20));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        Runnable withdrawMoneyTask = () -> {
            try {
                latch.await();
                client.withdraw(Currency.RUB, new BigDecimal(2));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        Runnable withdrawMoneyTaskAgain = () -> {
            try{
                latch.await();
                client.withdraw(Currency.RUB, new BigDecimal(3));
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        };

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            threads.add(new Thread(depositMoneyTask));
            threads.add(new Thread(withdrawMoneyTask));
            threads.add(new Thread(withdrawMoneyTaskAgain));
        }

        threads.forEach(Thread::start);
        latch.countDown();

        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Assertions.assertEquals(0, client.getAssets().get(Currency.RUB).compareTo(new BigDecimal(3750)));
    }
}
