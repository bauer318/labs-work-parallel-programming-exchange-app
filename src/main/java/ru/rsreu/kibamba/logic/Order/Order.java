package ru.rsreu.kibamba.logic.Order;

import ru.rsreu.kibamba.exception.InsufficientBalance;
import ru.rsreu.kibamba.logic.client.Client;
import ru.rsreu.kibamba.logic.currency.CurrencyPairs;
import ru.rsreu.kibamba.logic.currency.CurrencyWorker;

import java.math.BigDecimal;
import java.util.Objects;

public class Order {
    private final Client client;
    private final CurrencyPairs currencyPair;
    private final OrderType orderType;
    private OrderStatus orderStatus;
    private BigDecimal amount;
    private BigDecimal deposit;
    private final BigDecimal price;

    public Order(Client client, CurrencyPairs currencyPair, OrderType orderType, BigDecimal amount, BigDecimal price) {
        this.client = client;
        this.currencyPair = currencyPair;
        this.orderType = orderType;
        this.orderStatus = OrderStatus.REQUESTED;
        this.amount = amount.setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE);
        this.price = price.setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE);
        initOrder(client,currencyPair,orderType,amount,price);
    }
    private void initOrderBuy(Client client, CurrencyPairs currencyPair,BigDecimal amount, BigDecimal price){
        BigDecimal leftCurrencyBalance = client.getBalance().get(currencyPair.getLeftCurrency());
        BigDecimal amountCanBePurchased = leftCurrencyBalance.multiply(price)
                .setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE);
        BigDecimal amountPurchased = amount.divide(price,
                CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE);
        if (amountCanBePurchased.compareTo(amount
                .setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE)) >= 0) {
            this.orderStatus = OrderStatus.OPENED;
        }else{
            throw new InsufficientBalance(String.format("Cannot create order. Needed at least %s %s. Client %s has only %s",
                    amountPurchased, currencyPair.getLeftCurrency(), client.getId(), leftCurrencyBalance));
        }
    }
    private void initOrderSel(Client client, CurrencyPairs currencyPair,BigDecimal amount){
        BigDecimal rightCurrencyBalance = client.getBalance().get(currencyPair.getRightCurrency());
        BigDecimal rightCurrencyAmountToSold = amount
                .setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE);
        if(rightCurrencyBalance.compareTo(rightCurrencyAmountToSold) >= 0){
            this.orderStatus = OrderStatus.OPENED;
        }else{
            throw new InsufficientBalance(String.format("Cannot create order. Needed at least %s %s. Client %s has only %s",
                    rightCurrencyAmountToSold, currencyPair.getLeftCurrency(), client.getId(), rightCurrencyBalance));
        }
    }
    private void initOrder(Client client, CurrencyPairs currencyPair, OrderType orderType, BigDecimal amount, BigDecimal price){
        switch (orderType) {
            case BUY: {
                initOrderBuy(client,currencyPair,amount,price);
                break;
            }
            case SELL: {
                initOrderSel(client,currencyPair,amount);
                break;
            }
        }
    }

    public boolean compatibleWith(Order targetOrder){
        boolean isCompatiblePrice = false;
        switch (targetOrder.getOrderType()){
            case BUY: {
                if(this.getPrice().compareTo(targetOrder.getPrice())<=0){
                    isCompatiblePrice = true;
                }
            }
            case SELL:{
                if(this.getPrice().compareTo(targetOrder.getPrice())>=0){
                    isCompatiblePrice = true;
                }
            }
        }
        return this.orderType!=targetOrder.orderType &&
                this.currencyPair.equals(targetOrder.currencyPair) &&
                isCompatiblePrice &&
                this.client.getId()!=targetOrder.client.getId();

    }

    public void reduce(BigDecimal amount, BigDecimal price) {
        if (this.amount.compareTo(amount.setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE)) < 0) {
            throw new InsufficientBalance(String.format("Cannot withdraw order for %s. Current amount is %s", amount, this.amount));
        }
        BigDecimal newAmount = this.amount.subtract(amount).setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE);
        this.amount = newAmount;
        BigDecimal dealPrice = amount.multiply(price).setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE);
        switch (orderType) {
            case BUY: {
                client.deposit(currencyPair.getLeftCurrency(), amount);
                BigDecimal newDeposit = deposit.subtract(dealPrice);
                this.deposit = newDeposit;
                break;
            }
            case SELL: {
                client.deposit(currencyPair.getRightCurrency(), dealPrice);
                break;
            }
        }
    }
    public void revoke() {
        switch (orderType) {
            case BUY: {
                if (deposit.compareTo(BigDecimal.ZERO) > 0) {
                    client.deposit(currencyPair.getRightCurrency(), deposit);
                    this.deposit = BigDecimal.ZERO;
                }
                break;
            }
            case SELL: {
                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    client.deposit(currencyPair.getLeftCurrency(), amount);
                }
                break;
            }
        }
    }

    public Client getClient() {
        return client;
    }

    public CurrencyPairs getCurrencyPair() {
        return currencyPair;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public OrderStatus getOrderStatus(){return orderStatus;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(client, order.client) &&
                currencyPair == order.currencyPair &&
                orderType == order.orderType &&
                Objects.equals(amount, order.amount) &&
                Objects.equals(deposit, order.deposit) &&
                Objects.equals(price, order.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(client, currencyPair, orderType, amount, deposit, price);
    }
}
