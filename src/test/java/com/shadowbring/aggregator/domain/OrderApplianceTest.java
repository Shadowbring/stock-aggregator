package com.shadowbring.aggregator.domain;

import com.shadowbring.aggregator.domain.incoming.*;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OrderApplianceTest {

  private Map<Integer, Order> orders = new HashMap<>();

  @Test
  public void applyAddOrder() {
    AddOrder addOrder = generateAddOrder();
    addOrder.applyToOrderTable(orders);
    assertEquals("Size of the map with orders must be equal to 1", 1, orders.size());
    assertEquals("Orders must be equal", addOrder, orders.get(addOrder.getOrderId()));
  }

  @Test
  public void applyAddOrderThenChangeOrder() {
    generateAddOrder().applyToOrderTable(orders);
    generateChangeOrder().applyToOrderTable(orders);
    assertEquals("Size of the map with orders must be equal to 1", 1, orders.size());
    assertEquals("Product ID must be equal to 'Product'", "Product", ((AddOrder) orders.get(1)).getProductId());
    assertEquals("Price must be equal to 7", new Integer(7), ((AddOrder) orders.get(1)).getPrice());
    assertEquals("Quantity must be equal to 7", new Integer(7), ((AddOrder) orders.get(1)).getQuantity());
    assertEquals("Side must be equal to 'buy'", Side.buy, ((AddOrder) orders.get(1)).getSide());
  }

  @Test
  public void applyAddOrderThenChangeOrderNoChanges() {
    AddOrder addOrder = generateAddOrder();
    addOrder.applyToOrderTable(orders);
    ChangeOrder changeOrder = generateChangeOrder();
    changeOrder.setOrderId(2);
    changeOrder.applyToOrderTable(orders);
    assertEquals("Size of the map with orders must be equal to 1", 1, orders.size());
    assertEquals("Orders must be equal", addOrder, orders.get(addOrder.getOrderId()));
  }

  @Test
  public void applyAddOrderThanDeleteOrder() {
    generateChangeOrder().applyToOrderTable(orders);
    generateDeleteOrder().applyToOrderTable(orders);
    assertTrue("Map with orders must be empty", orders.isEmpty());
  }

  @Test
  public void applyAddOrderThanDeleteOrderNoChanges() {
    AddOrder addOrder = generateAddOrder();
    addOrder.applyToOrderTable(orders);
    DeleteOrder deleteOrder = generateDeleteOrder();
    deleteOrder.setOrderId(2);
    deleteOrder.applyToOrderTable(orders);
    assertEquals("Size of the map with orders must be equal to 1", 1, orders.size());
    assertEquals("Orders must be equal", addOrder, orders.get(addOrder.getOrderId()));
  }

  private AddOrder generateAddOrder() {
    AddOrder addOrder = new AddOrder();
    addOrder.setOrderId(1);
    addOrder.setProductId("Product");
    addOrder.setPrice(3);
    addOrder.setQuantity(3);
    addOrder.setSide(Side.buy);

    return addOrder;
  }

  private ChangeOrder generateChangeOrder() {
    ChangeOrder changeOrder = new ChangeOrder();
    changeOrder.setOrderId(1);
    changeOrder.setPrice(7);
    changeOrder.setQuantity(7);

    return changeOrder;
  }

  private DeleteOrder generateDeleteOrder() {
    DeleteOrder deleteOrder = new DeleteOrder();
    deleteOrder.setOrderId(1);

    return deleteOrder;
  }
}
