package com.shadowbring.aggregator.domain.outgoing;

import lombok.Data;

/**
 * POJO-class that represents the buy/sell level of the {@link Product}
 *
 * @author Dmytro Bezruk
 */
@Data
public class Level {

  private Integer price;

  private Integer quantity;
}
