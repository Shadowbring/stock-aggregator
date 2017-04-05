package com.shadowbring.aggregator.domain.incoming;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Map;

/**
 * Marker interface that simplifies mapping of the JSON messages to the defined POJO-classes. Also defines the contract
 * for the interaction between {@link Order} and its store
 *
 * @author Dmytro Bezuk
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
                  @JsonSubTypes.Type(value = AddOrder.class, name = "addOrder"),
                  @JsonSubTypes.Type(value = ChangeOrder.class, name = "changeOrder"),
                  @JsonSubTypes.Type(value = DeleteOrder.class, name = "deleteOrder")
})
public interface Order {

  /**
   * This method defines the contract for the interaction between {@link Order} and its store
   *
   * @param orders - storage of the {@link Order}s
   */
  void applyToOrderTable(Map<Integer, Order> orders);
}
