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
        BigDecimal leftCurrencyBalance = client.getAssets().get(currencyPair.getLeftCurrency());
        BigDecimal maxLeftCurrencyToBuy = client.getMaxLeftCurrencyToBuy(currencyPair,price);
        BigDecimal amountPurchased = amount.divide(price,
                CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE);
        if (maxLeftCurrencyToBuy
                .compareTo(amount.setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE)) >= 0) {
            this.orderStatus = OrderStatus.OPENED;
        }else{
            throw new InsufficientBalance(String.format("Cannot create order. Needed at least %s %s. Client %s has only %s",
                    amountPurchased, currencyPair.getLeftCurrency(), client.getId(), leftCurrencyBalance));
        }
    }
    private void initOrderSel(Client client, CurrencyPairs currencyPair,BigDecimal amount){
        BigDecimal leftCurrencyBalance = client.getAssets().get(currencyPair.getLeftCurrency());
        BigDecimal rightCurrencyBalance = client.getAssets().get(currencyPair.getRightCurrency());
        BigDecimal leftCurrencyBalanceToSold = amount
                .setScale(CurrencyWorker.CURRENCY_SCALE, CurrencyWorker.CURRENCY_ROUNDING_MODE);
        if(leftCurrencyBalance.compareTo(leftCurrencyBalanceToSold) >= 0){
            this.orderStatus = OrderStatus.OPENED;
        }else{
            throw new InsufficientBalance(String.format("Cannot create order. Needed at least %s %s. Client %s has only %s",
                    leftCurrencyBalanceToSold, currencyPair.getLeftCurrency(), client.getId(), rightCurrencyBalance));
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
        boolean canBuyWithTargetPrice = true;
        if(this.orderType==OrderType.BUY){
            canBuyWithTargetPrice = this.secondOrderCanBuyWithTargetPrice(targetOrder);
        }
        return this.orderType!=targetOrder.orderType &&
                this.currencyPair.equals(targetOrder.currencyPair) &&
                this.amount.compareTo(targetOrder.amount)==0 &&
                this.client.getId()!=targetOrder.client.getId() &&
                canBuyWithTargetPrice &&
                this.orderStatus==OrderStatus.OPENED &&
                targetOrder.orderStatus == OrderStatus.OPENED;
    }


    private boolean secondOrderCanBuyWithTargetPrice(Order target){
        BigDecimal maxAmountCanBuy = this.client.getMaxLeftCurrencyToBuy(target.currencyPair,target.price);
        return maxAmountCanBuy.compareTo(this.amount)>=0;
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
    public void setOrderStatus(OrderStatus orderStatus){
        this.orderStatus = orderStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(client, order.client) &&
                currencyPair == order.currencyPair &&
                orderType == order.orderType &&
                Objects.equals(amount, order.amount) &&
                Objects.equals(price, order.price) &&
                orderStatus==order.orderStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(client, currencyPair, orderType, amount, price,orderStatus);
    }
}
