package ClientTest;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import  org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import ru.rsreu.kibamba.exception.InsufficientBalance;
import ru.rsreu.kibamba.logic.Client;
import ru.rsreu.kibamba.logic.Currency;
import ru.rsreu.kibamba.logic.CurrencyPairs;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class TestClient {

    @Test
    public void testBuyRUBWithUSD(){
        Client buyer = new Client(1);
        BigDecimal amountDeposit = new BigDecimal(300);
        BigDecimal amountBuy = new BigDecimal(50);
        BigDecimal priceUSDToRUB = new BigDecimal("62.44");
        buyer.deposit(Currency.USD,amountDeposit);
        Assertions.assertEquals(0, buyer.getBalance().get(Currency.USD).compareTo(amountDeposit));
        Assertions.assertEquals(0, buyer.getBalance().get(Currency.RUB).compareTo(BigDecimal.ZERO));
        buyer.buy(CurrencyPairs.USD_RUB,amountBuy,priceUSDToRUB);
        Assertions.assertEquals(0, buyer.getBalance().get(Currency.RUB).compareTo(new BigDecimal(3122)));
        Assertions.assertEquals(0, buyer.getBalance().get(Currency.USD).compareTo(new BigDecimal(250)));
    }

    @Test
    public void testSelUSDWithRUB(){
        Client seller = new Client(1);
        BigDecimal amountDepositUSD = new BigDecimal(250);
        BigDecimal amountDepositRUB = new BigDecimal(3122);
        BigDecimal amountSold = new BigDecimal(2000);
        BigDecimal priceRUBToUSD = new BigDecimal("0.02");
        seller.deposit(Currency.USD,amountDepositUSD);
        seller.deposit(Currency.RUB,amountDepositRUB);
        Assertions.assertEquals(0, seller.getBalance().get(Currency.USD).compareTo(amountDepositUSD));
        Assertions.assertEquals(0, seller.getBalance().get(Currency.RUB).compareTo(amountDepositRUB));
        seller.sel(CurrencyPairs.USD_RUB,amountSold,priceRUBToUSD);
        Assertions.assertEquals(0, seller.getBalance().get(Currency.RUB).compareTo(new BigDecimal(1122)));
        Assertions.assertEquals(0, seller.getBalance().get(Currency.USD).compareTo(new BigDecimal(290)));
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
