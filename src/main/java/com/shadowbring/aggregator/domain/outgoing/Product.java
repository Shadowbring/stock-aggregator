package com.shadowbring.aggregator.domain.outgoing;

import lombok.Data;

import java.util.List;

/**
 * POJO-class that represents the aggregated price levels for the particular product
 *
 * @author Dmytro Bezruk
 */
@Data
public class Product {

  private String productId;

  private List<Level> buyLevels;

  private List<Level> sellLevels;
}
