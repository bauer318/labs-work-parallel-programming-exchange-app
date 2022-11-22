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
    public void testBuyRUBWithUSD(){
        Client buyer = new Client(1);
        BigDecimal amountDeposit = new BigDecimal(300);
        BigDecimal amountBuy = new BigDecimal(3000);
        BigDecimal priceRUBToUSD = new BigDecimal("0.01");
        buyer.deposit(Currency.USD,amountDeposit);
        Assertions.assertEquals(0, buyer.getBalance().get(Currency.USD).compareTo(amountDeposit));
        Assertions.assertEquals(0, buyer.getBalance().get(Currency.RUB).compareTo(BigDecimal.ZERO));
        buyer.buy(CurrencyPairs.USD_RUB,amountBuy,priceRUBToUSD);
        Assertions.assertEquals(0, buyer.getBalance().get(Currency.RUB).compareTo(amountBuy));
        Assertions.assertEquals(0, buyer.getBalance().get(Currency.USD).compareTo(new BigDecimal(270)));
    }

    @Test
    public void testSelUSDGetRUB(){
        Client seller = new Client(1);
        BigDecimal amountDepositUSD = new BigDecimal(270);
        BigDecimal amountDepositRUB = new BigDecimal(3000);
        BigDecimal amountSold = new BigDecimal(70);
        BigDecimal priceUSDToRUB = new BigDecimal(62);
        seller.deposit(Currency.USD,amountDepositUSD);
        seller.deposit(Currency.RUB,amountDepositRUB);
        Assertions.assertEquals(0, seller.getBalance().get(Currency.USD).compareTo(amountDepositUSD));
        Assertions.assertEquals(0, seller.getBalance().get(Currency.RUB).compareTo(amountDepositRUB));
        seller.sel(CurrencyPairs.USD_RUB,amountSold,priceUSDToRUB);
        Assertions.assertEquals(0, seller.getBalance().get(Currency.RUB).compareTo(new BigDecimal(7340)));
        Assertions.assertEquals(0, seller.getBalance().get(Currency.USD).compareTo(new BigDecimal(200)));
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

        Assertions.assertEquals(0, client.getBalance().get(Currency.RUB).compareTo(new BigDecimal(3750)));
    }
}
