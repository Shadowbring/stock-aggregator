package com.shadowbring.aggregator.aggregation;

import com.shadowbring.aggregator.domain.incoming.AddOrder;
import com.shadowbring.aggregator.domain.incoming.MessageSequence;
import com.shadowbring.aggregator.domain.incoming.Order;
import com.shadowbring.aggregator.domain.incoming.Side;
import com.shadowbring.aggregator.udp.UdpCompliantMessageSequenceBuffer;
import io.vertx.core.json.Json;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PriceLevelsAggregatorTest {

  private static final String EXPECTED_AGGREGATION_RESULT = "[{\"outSequenceNumber\":1,\"products\":[{\"productId\":" +
                                                                "\"Product5\",\"buyLevels\":[{\"price\":7,\"quantity" +
                                                                "\":6}],\"sellLevels\":[]},{\"productId\":\"Product6" +
                                                                "\",\"buyLevels\":[{\"price\":8,\"quantity\":7}],\"se" +
                                                                "llLevels\":[]},{\"productId\":\"Product3\",\"buyLeve" +
                                                                "ls\":[{\"price\":5,\"quantity\":4}],\"sellLevels\":" +
                                                                "[]},{\"productId\":\"Product4\",\"buyLevels\":[{\"pr" +
                                                                "ice\":6,\"quantity\":5}],\"sellLevels\":[]},{\"prod" +
                                                                "uctId\":\"Product1\",\"buyLevels\":[{\"price\":3,\"" +
                                                                "quantity\":2}],\"sellLevels\":[]}]},{\"outSequenceN" +
                                                                "umber\":2,\"products\":[{\"productId\":\"Product2\"" +
                                                                ",\"buyLevels\":[{\"price\":4,\"quantity\":3}],\"sel" +
                                                                "lLevels\":[]},{\"productId\":\"Product0\",\"buyLeve" +
                                                                "ls\":[{\"price\":2,\"quantity\":1}],\"sellLevels\":[]}]}]";

  @Autowired
  private PriceLevelsAggregator aggregator;

  @Autowired
  private UdpCompliantMessageSequenceBuffer buffer;

  private static MessageSequence generateSequence() {
    MessageSequence messageSequence = new MessageSequence();
    List<Order> orders = new ArrayList<>();
    messageSequence.setInSequenceNumber(1);
    for (int i = 0; i < 7; i++) {
      AddOrder addOrder = new AddOrder();
      addOrder.setOrderId(i + 1);
      addOrder.setProductId("Product" + i);
      addOrder.setPrice(i + 2);
      addOrder.setQuantity(i + 1);
      addOrder.setSide(Side.buy);

      orders.add(addOrder);
    }
    messageSequence.setMessages(orders);
    return messageSequence;
  }

  @Before
  public void setUp() {
    buffer.addMessageSequence(generateSequence());
  }

  @Test
  public void aggregateByPriceLevels() {
    assertEquals("Results of aggregation must be equal", EXPECTED_AGGREGATION_RESULT,
        Json.encode(aggregator.aggregateByPriceLevels()));
  }
}