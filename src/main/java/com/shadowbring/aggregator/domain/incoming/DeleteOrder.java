package com.shadowbring.aggregator.domain.incoming;

import lombok.Data;

import java.util.Map;

/**
 * POJO-class that represents the message that deletes existing {@link Order}
 *
 * @author Dmytro Bezruk
 */
@Data
public class DeleteOrder implements Order {

  private Integer orderId;

  /**
   * This method looks for the {@link Order} with the particular ID in the {@link Map}. If found - deletes it.
   * Nothing otherwise
   *
   * @param orders - destination where this instance must delete an existing order
   */
  @Override
  public void applyToOrderTable(Map<Integer, Order> orders) {
    if (orders.containsKey(this.orderId)) {
      orders.remove(this.orderId);
    }
  }
}
