package com.shadowbring.aggregator.domain.outgoing;

import lombok.Data;

import java.util.List;

/**
 * POJO-class that represents the part of the list with aggregated price levels. Its JSON projection will be transferred
 * via UDP
 *
 * @author Dmytro Bezruk
 */
@Data
public class ProductsBulk {
  /**
   * Serial number of the bulk. Since UDP doesn't guarantee persistence of the ordering it will help to reconstruct the
   * original order
   */
  private Integer outSequenceNumber;

  /**
   * Aggregated price levels for the particular product
   */
  private List<Product> products;
}
