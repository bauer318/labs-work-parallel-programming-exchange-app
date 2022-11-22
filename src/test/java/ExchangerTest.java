import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.rsreu.kibamba.logic.Exchanger;
import ru.rsreu.kibamba.logic.Order.Order;
import ru.rsreu.kibamba.logic.Order.OrderType;
import ru.rsreu.kibamba.logic.currency.Currency;
import ru.rsreu.kibamba.logic.currency.CurrencyPairs;
import ru.rsreu.kibamba.logic.currency.CurrencyWorker;
import ru.rsreu.kibamba.logic.client.Client;


import java.math.BigDecimal;

public class ExchangerTest {

    private Exchanger exchanger;

    @Before
    public void before() {
        exchanger = new Exchanger();
    }

    @Test
    public void addOrderTest() {
        Client client = new Client(1);
        client.deposit(Currency.RUB, new BigDecimal(1000));
        Order order = new Order(client, CurrencyPairs.USD_RUB, OrderType.BUY, new BigDecimal(15), new BigDecimal(66.66));
        exchanger.addOrder(order);
        Assert.assertEquals(1, exchanger.getAllOrdersList().size());
    }

    @Test
    public void addTwoCompletelyMatchingOrdersTest() {
        Client client1 = new Client(1);
        client1.deposit(Currency.RUB, new BigDecimal(1000));
        Order order1 = new Order(client1, CurrencyPairs.USD_RUB, OrderType.BUY, new BigDecimal(15), new BigDecimal(66.66));
        exchanger.addOrder(order1);

        Client client2 = new Client(2);
        client2.deposit(Currency.USD, new BigDecimal(15));
        Order order2 = new Order(client2, CurrencyPairs.USD_RUB, OrderType.SELL, new BigDecimal(15), new BigDecimal(65));
        exchanger.addOrder(order2);

        Assert.assertEquals(0, exchanger.getAllOrdersList().size());
        Assert.assertEquals(0, client1.getBalance().get(Currency.USD).compareTo(new BigDecimal(15)));
        Assert.assertEquals(0, client2.getBalance().get(Currency.RUB)
                .compareTo(new BigDecimal(999.90)
                        .setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE)));
    }

    @Test
    public void addTwoMatchingOrdersWithDepositRevokedTest() {
        Client client1 = new Client(1);
        client1.deposit(Currency.USD, new BigDecimal(15));
        Order order1 = new Order(client1, CurrencyPairs.USD_RUB, OrderType.SELL, new BigDecimal(15), new BigDecimal(65));
        exchanger.addOrder(order1);

        Client client2 = new Client(2);
        client2.deposit(Currency.RUB, new BigDecimal(1000));
        Order order2 = new Order(client2, CurrencyPairs.USD_RUB, OrderType.BUY, new BigDecimal(15), new BigDecimal(66.66));
        exchanger.addOrder(order2);

        Assert.assertEquals(0, exchanger.getAllOrdersList().size());
        Assert.assertEquals(0, client1.getBalance().get(Currency.RUB)
                .compareTo(new BigDecimal(975).setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE)));
        Assert.assertEquals(0, client2.getBalance().get(Currency.USD).compareTo(new BigDecimal(15)));
        Assert.assertEquals(0, client2.getBalance().get(Currency.RUB).compareTo(new BigDecimal(25)));
    }

    @Test
    public void addOrderMatchingTwoAndClosingThemTest() {
        Client client1 = new Client(1);
        client1.deposit(Currency.USD, new BigDecimal(15));
        Order order1 = new Order(client1, CurrencyPairs.USD_RUB, OrderType.SELL, new BigDecimal(15), new BigDecimal(65));
        exchanger.addOrder(order1);

        Client client2 = new Client(2);
        client2.deposit(Currency.USD, new BigDecimal(25));
        Order order2 = new Order(client2, CurrencyPairs.USD_RUB, OrderType.SELL, new BigDecimal(25), new BigDecimal(70));
        exchanger.addOrder(order2);

        Assert.assertEquals(2, exchanger.getAllOrdersList().size());

        Client client3 = new Client(3);
        client3.deposit(Currency.RUB, new BigDecimal(10_000));
        Order order3 = new Order(client3, CurrencyPairs.USD_RUB, OrderType.BUY, new BigDecimal(40), new BigDecimal(80));
        exchanger.addOrder(order3);

        Assert.assertEquals(0, exchanger.getAllOrdersList().size());
        Assert.assertEquals(0, client1.getBalance().get(Currency.RUB).compareTo(new BigDecimal(975)));
        Assert.assertEquals(0, client2.getBalance().get(Currency.RUB).compareTo(new BigDecimal(1750)));
        Assert.assertEquals(0, client3.getBalance().get(Currency.USD).compareTo(new BigDecimal(40)));
        Assert.assertEquals(0, client3.getBalance().get(Currency.RUB).compareTo(new BigDecimal(7275)));

    }

    @Test
    public void addOrderMatchingTwoAndClosingOneTest() {
        Client client1 = new Client(1);
        client1.deposit(Currency.USD, new BigDecimal(15));
        Order order1 = new Order(client1, CurrencyPairs.USD_RUB, OrderType.SELL, new BigDecimal(15), new BigDecimal(65));
        exchanger.addOrder(order1);

        Client client2 = new Client(2);
        client2.deposit(Currency.USD, new BigDecimal(25));
        Order order2 = new Order(client2, CurrencyPairs.USD_RUB, OrderType.SELL, new BigDecimal(25), new BigDecimal(70));
        exchanger.addOrder(order2);

        Assert.assertEquals(2, exchanger.getAllOrdersList().size());

        Client client3 = new Client(3);
        client3.deposit(Currency.RUB, new BigDecimal(10_000));
        Order order3 = new Order(client3, CurrencyPairs.USD_RUB, OrderType.BUY, new BigDecimal(25), new BigDecimal(80));
        exchanger.addOrder(order3);

        Assert.assertEquals(1, exchanger.getAllOrdersList().size());
        Assert.assertEquals(0, client1.getBalance().get(Currency.RUB).compareTo(new BigDecimal(975)));
        Assert.assertEquals(0, client2.getBalance().get(Currency.RUB).compareTo(new BigDecimal(700)));
        Assert.assertEquals(0, client3.getBalance().get(Currency.USD).compareTo(new BigDecimal(25)));
        Assert.assertEquals(0, client3.getBalance().get(Currency.RUB).compareTo(new BigDecimal(8325)));
    }

    @Test
    public void addTwoOrdersNotMatchingByPriceTest() {
        Client client1 = new Client(1);
        client1.deposit(Currency.USD, new BigDecimal(15));
        Order order1 = new Order(client1, CurrencyPairs.USD_RUB, OrderType.SELL, new BigDecimal(15), new BigDecimal(65));
        exchanger.addOrder(order1);

        Client client2 = new Client(3);
        client2.deposit(Currency.RUB, new BigDecimal(10_000));
        Order order2 = new Order(client2, CurrencyPairs.USD_RUB, OrderType.BUY, new BigDecimal(25), new BigDecimal(60));
        exchanger.addOrder(order2);

        Assert.assertEquals(2, exchanger.getAllOrdersList().size());
    }

}
