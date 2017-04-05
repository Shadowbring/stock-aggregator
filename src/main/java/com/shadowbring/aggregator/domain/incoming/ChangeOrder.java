package com.shadowbring.aggregator.domain.incoming;

import lombok.Data;

import java.util.Map;

/**
 * POJO-class that represents the message that updates existing {@link Order}
 *
 * @author Dmytro Bezruk
 */
@Data
public class ChangeOrder implements Order {

  private Integer orderId;

  private Integer price;

  private Integer quantity;

  /**
   * This method looks for the {@link Order} with the particular ID in the {@link Map}. If found - updates particular
   * {@link Order}. Nothing otherwise
   *
   * @param orders - destination where this instance must apply its updates
   */
  @Override
  public void applyToOrderTable(Map<Integer, Order> orders) {
    if (orders.containsKey(this.orderId)) {
      orders.compute(this.orderId, (id, order) -> {
        AddOrder updatedOrder = (AddOrder) order;
        updatedOrder.setPrice(this.price);
        updatedOrder.setQuantity(this.quantity);

        return updatedOrder;
      });
    }
  }
}
