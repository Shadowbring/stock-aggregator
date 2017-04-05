package com.shadowbring.aggregator.domain.incoming;

import lombok.Data;

import java.util.Map;

/**
 * POJO-class that represents the message with the new order
 *
 * @author Dmytro Bezruk
 */
@Data
public class AddOrder implements Order {

  private Integer orderId;

  private String productId;

  private Side side;

  private Integer price;

  private Integer quantity;

  /**
   * Method that adds current instance to the {@link Order}s' store
   *
   * @param orders - destination where this instance must be added
   */
  @Override
  public void applyToOrderTable(Map<Integer, Order> orders) {
    orders.put(this.orderId, this);
  }
}
