package com.shadowbring.aggregator.domain.incoming;

import lombok.Data;

import java.util.List;

/**
 * POJO-class that represents the sequence of the received {@link Order}s
 *
 * @author Dmytro Bezruk
 */
@Data
public class MessageSequence {

  /**
   * Serial number of the sequence
   */
  private Integer inSequenceNumber;

  /**
   * Sequence of the {@link Order}s
   */
  private List<Order> messages;
}
