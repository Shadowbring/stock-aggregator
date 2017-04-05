package com.shadowbring.aggregator.aggregation;

import com.shadowbring.aggregator.domain.incoming.AddOrder;
import com.shadowbring.aggregator.domain.incoming.MessageSequence;
import com.shadowbring.aggregator.domain.incoming.Order;
import com.shadowbring.aggregator.domain.incoming.Side;
import com.shadowbring.aggregator.domain.outgoing.Level;
import com.shadowbring.aggregator.domain.outgoing.Product;
import com.shadowbring.aggregator.domain.outgoing.ProductsBulk;
import com.shadowbring.aggregator.udp.UdpCompliantMessageSequenceBuffer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

/**
 * This service was designed for processing of the {@link MessageSequence}s, that were previously ordered and cleaned up
 * by the {@link UdpCompliantMessageSequenceBuffer}, extraction of the {@link Order}s from the sequences, updating of
 * the {@link Order}s' storage and aggregation that will be specified below.
 * <p>
 * As a result of its work, provides chunks of the price levels table that are ready for transmission via UDP.
 *
 * @author Dmytro Bezruk
 */
@Component
@Slf4j
public class PriceLevelsAggregator {

  /**
   * Storage of the extracted orders. {@link Map} provides convenient lookup by OrderId at a constant time.
   */
  private final Map<Integer, Order> orders = new HashMap<>();

  /**
   * Size of the bulks that aggregated price levels will be split in. Configurable via application.properties file
   */
  @Value("${price-levels.bulk-size}")
  private Integer bulkSize;
  /**
   * Buffer that performs ordering of the received {@link MessageSequence}s and removes duplicates
   */
  @Autowired
  private UdpCompliantMessageSequenceBuffer buffer;

  /**
   * Extracts all orders from the buffer and stores them in the {@link Map}
   */
  private void updateOrders() {
    buffer.flush().stream()
        .map(MessageSequence::getMessages)
        .forEach(orderList -> orderList.forEach(order -> order.applyToOrderTable(orders)));
    log.info("Actual size of the orders table is [{}] items", orders.size());
  }

  /**
   * Method that is invoked by the {@link com.shadowbring.aggregator.listener.StockListener} when it is time to process
   * and aggregate all {@link MessageSequence}s accumulated in the {@link UdpCompliantMessageSequenceBuffer}. It
   * calculates sell and buy price levels per single product, sorts sell price levels in ascending order and buy price
   * levels in descending one.
   *
   * @return all aggregated price levels per product that are split in bulks for further transmission
   */
  public List<ProductsBulk> aggregateByPriceLevels() {
    List<Product> products = new ArrayList<>();
    updateOrders();
    Map<String, Map<Side, List<AddOrder>>> byProductIdAndSide = groupOrdersBySide(groupOrdersByProductId());
    aggregateProductPriceLevels(products, byProductIdAndSide);
    return mapPriceLevelBulksToDtoList(ListUtils.partition(products, bulkSize));
  }

  /**
   * Maps partitioned and aggregated price levels to POJO-class that will be converted to JSON later
   *
   * @param partitionedProducts - aggregated price levels that are split in equal parts
   * @return {@link List<ProductsBulk>} of the data transfer objects
   */
  private List<ProductsBulk> mapPriceLevelBulksToDtoList(List<List<Product>> partitionedProducts) {
    return partitionedProducts.stream()
               .map(productsList -> {
                 ProductsBulk bulk = new ProductsBulk();
                 bulk.setOutSequenceNumber(partitionedProducts.indexOf(productsList) + 1);
                 bulk.setProducts(productsList);
                 return bulk;
               })
               .collect(Collectors.toList());
  }

  /**
   * Accumulates price levels in the {@link List<Product>} for further partitioning
   *
   * @param products           - {@link List<Product>} where price levels are accumulated
   * @param byProductIdAndSide - {@link Order}s that are grouped by product ID and then by sell/buy side
   */
  private void aggregateProductPriceLevels(List<Product> products,
                                           Map<String, Map<Side, List<AddOrder>>> byProductIdAndSide) {
    byProductIdAndSide.forEach((productId, sideListMap) -> {
      Product product = new Product();
      List<Level> buyLevels = new ArrayList<>();
      List<Level> sellLevels = new ArrayList<>();
      product.setProductId(productId);

      sideListMap.forEach((side, ordersForSide) -> {
        switch (side) {
          case buy:
            calculateLevelsForProduct(buyLevels, ordersForSide);
            break;
          case sell:
            calculateLevelsForProduct(sellLevels, ordersForSide);
            break;
        }
      });

      sellLevels.sort(Comparator.comparingInt(Level::getPrice));
      buyLevels.sort((level1, level2) -> level2.getPrice() - level1.getPrice());

      product.setBuyLevels(buyLevels);
      product.setSellLevels(sellLevels);

      products.add(product);
    });
  }

  /**
   * Method that groups by sell/buy side {@link Order}s that are already grouped by product ID
   *
   * @param ordersByProductId - {@link Order}s that are already grouped by product ID
   * @return - {@link Order}s that are grouped by product ID and then by sell/buy side
   */
  private Map<String, Map<Side, List<AddOrder>>> groupOrdersBySide(Map<String, List<AddOrder>> ordersByProductId) {
    return ordersByProductId.entrySet().stream()
               .map(addSubGroupingBySide())
               .collect(toMap(AbstractMap.SimpleEntry::getKey,
                   AbstractMap.SimpleEntry::getValue));
  }

  /**
   * Method that groups all {@link Order}s by product ID
   *
   * @return - {@link Map} where {@link Order}s are grouped by product ID
   */
  private Map<String, List<AddOrder>> groupOrdersByProductId() {
    return orders.values()
               .stream()
               .map(order -> ((AddOrder) order))
               .collect(groupingBy(AddOrder::getProductId));
  }

  /**
   * Calculates sell/buy levels for a particular product
   *
   * @param levels        - {@link List<Level>} where levels are accumulated
   * @param ordersForSide - only sell or buy {@link Order}s
   */
  private void calculateLevelsForProduct(List<Level> levels, List<AddOrder> ordersForSide) {
    Map<Integer, List<AddOrder>> ordersByPrice = ordersForSide.stream()
                                                     .collect(groupingBy(AddOrder::getPrice));
    ordersByPrice.entrySet().stream()
        .map(mapOrderToPriceLevel())
        .forEach(levels::add);
  }

  /**
   * Function that maps particular sell or buy orders to price level
   *
   * @return - mapping function
   */
  private Function<Map.Entry<Integer, List<AddOrder>>, Level> mapOrderToPriceLevel() {
    return entry -> {
      Level level = new Level();
      level.setPrice(entry.getKey());
      level.setQuantity(entry.getValue().stream()
                            .map(AddOrder::getQuantity)
                            .reduce(0, (q1, q2) -> q1 + q2));

      return level;
    };
  }

  /**
   * Function that adds grouping of the {@link Order}s by sell or buy side
   *
   * @return - mapping function
   */
  private Function<Map.Entry<String, List<AddOrder>>, AbstractMap.SimpleEntry<String, Map<Side, List<AddOrder>>>>
  addSubGroupingBySide() {
    return entry -> new HashMap.SimpleEntry<>(entry.getKey(),
                                                 entry.getValue().stream().collect(groupingBy(AddOrder::getSide)));
  }
}
