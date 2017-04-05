package com.shadowbring.aggregator.udp;

import com.shadowbring.aggregator.domain.incoming.MessageSequence;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NavigableSet;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UdpCompliantMessageSequenceBufferTest {

  @Autowired
  private UdpCompliantMessageSequenceBuffer buffer;

  @Test
  public void initCache() {
    assertNotNull("Cache must not be null", buffer.getCache());
  }

  @Test
  public void addMessageSequence() {
    MessageSequence messageSequence = new MessageSequence();
    messageSequence.setInSequenceNumber(1);
    buffer.addMessageSequence(messageSequence);
    assertEquals("Size of the buffer must be equal to 1", 1, buffer.getOrderedMessageSequenceBuffer().size());
    assertEquals("Message sequences must be equal", messageSequence, buffer.getOrderedMessageSequenceBuffer().last());
  }

  @Test
  public void addDisorderedMessageSequences() {
    MessageSequence messageSequence = new MessageSequence();
    messageSequence.setInSequenceNumber(1);
    buffer.addMessageSequence(messageSequence);

    MessageSequence messageSequence3 = new MessageSequence();
    messageSequence3.setInSequenceNumber(3);
    buffer.addMessageSequence(messageSequence3);

    assertEquals("OrderedMessageSequenceBuffer must contain 1 element", 1,
        buffer.getOrderedMessageSequenceBuffer().size());
    assertTrue("OrderedMessageSequenceBuffer must contain messageSequence",
        buffer.getOrderedMessageSequenceBuffer().contains(messageSequence));
    assertEquals("Cache must contain 1 element", 1, buffer.getCache().size());
    assertTrue("Cache must contain messageSequence3", buffer.getCache().containsValue(messageSequence3));

    MessageSequence messageSequence2 = new MessageSequence();
    messageSequence2.setInSequenceNumber(2);
    buffer.addMessageSequence(messageSequence2);

    assertEquals("OrderedMessageSequenceBuffer must contain 3 elements", 3,
        buffer.getOrderedMessageSequenceBuffer().size());
    assertTrue("Cache must be empty now", buffer.getCache().isEmpty());
    assertTrue("OrderedMessageSequenceBuffer must contain all these messages",
        buffer.getOrderedMessageSequenceBuffer().containsAll(Arrays.asList(messageSequence, messageSequence2,
            messageSequence3)));
    List<MessageSequence> list = new ArrayList<>(buffer.getOrderedMessageSequenceBuffer());
    assertEquals("MessageSequence must has index 0", messageSequence, list.get(0));
    assertEquals("MessageSequence2 must has index 1", messageSequence2, list.get(1));
    assertEquals("MessageSequence3 must has index 2", messageSequence3, list.get(2));
  }

  @Test
  public void flush() {
    MessageSequence messageSequence = new MessageSequence();
    messageSequence.setInSequenceNumber(1);

    MessageSequence messageSequence2 = new MessageSequence();
    messageSequence2.setInSequenceNumber(2);

    buffer.addMessageSequence(messageSequence);
    buffer.addMessageSequence(messageSequence2);

    NavigableSet<MessageSequence> flushedContent = buffer.flush();

    assertTrue("Cache must be empty", buffer.getCache().isEmpty());
    assertTrue("OrderedMessageSequenceBuffer must be empty", buffer.getOrderedMessageSequenceBuffer().isEmpty());

    assertTrue("Flushed content must contain messageSequence", flushedContent.contains(messageSequence));
    assertTrue("Flushed content must contain messageSequence2", flushedContent.contains(messageSequence2));
  }

  @Test
  public void getCache() {
    assertNotNull("Cache must not be null", buffer.getCache());
  }

  @Test
  public void getOrderedMessageSequenceBuffer() {
    assertNotNull("OrderedMessageSequenceBuffer must not be null", buffer.getOrderedMessageSequenceBuffer());
  }

  @After
  public void tearDown() {
    buffer.flush();
  }

}