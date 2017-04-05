package com.shadowbring.aggregator.udp;

import com.shadowbring.aggregator.domain.incoming.MessageSequence;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * This service acts like a temporal storage for the incoming messages and mitigates data distortion that was caused by
 * the nature of the UDP protocol
 *
 * @author Dmytro Bezruk
 */
@Component
@Slf4j
public class UdpCompliantMessageSequenceBuffer {

  /**
   * The back-end of this buffer. Features of {@link TreeMap} allow to skip duplicates and reorder the messages
   * correctly in case of properly implemented {@link Comparator}
   */
  private final NavigableSet<MessageSequence> orderedMessageSequenceBuffer =
      Collections
          .synchronizedNavigableSet(new TreeSet<>(Comparator.comparingInt(MessageSequence::getInSequenceNumber)));

  /**
   * Capacity of the cache. Configurable via application.properties file
   */
  @Value("${cache.capacity}")
  private Integer cacheCapacity;

  /**
   * Back-end of the cache
   */
  private Map<Integer, MessageSequence> cache;

  /**
   * Initialization of the cache with the already injected property
   */
  @PostConstruct
  public void initCache() {
    cache = Collections.synchronizedMap(new HashMap<>(cacheCapacity, 1));
  }

  /**
   * Adds {@link MessageSequence} to the buffer if its {@link MessageSequence#inSequenceNumber} is expected (there are
   * no skipped numbers). Otherwise, its placed to the cache
   *
   * @param messageSequence - {@link MessageSequence} to be added
   */
  public void addMessageSequence(MessageSequence messageSequence) {
    if (isSequenceNumberExpected(messageSequence)) {
      orderedMessageSequenceBuffer.add(messageSequence);
      log.info("Buffer size: " + orderedMessageSequenceBuffer.size());
      log.info("Buffer contents: " + orderedMessageSequenceBuffer);
      lookupNextSequencesInCache(messageSequence);
    } else {
      putInCache(messageSequence);
    }
  }

  /**
   * Transfers all data from this buffer to {@link com.shadowbring.aggregator.aggregation.PriceLevelsAggregator}.
   * Removes all data from the backing {@link TreeSet}, clears cache
   *
   * @return - all {@link MessageSequence}s accumulated during the current synchronization cycle
   */
  public NavigableSet<MessageSequence> flush() {
    NavigableSet<MessageSequence> content = new TreeSet<>(orderedMessageSequenceBuffer);
    orderedMessageSequenceBuffer.removeAll(content);
    cache.clear();

    return content;
  }

  /**
   * Puts the {@link MessageSequence} with the unexpected {@link MessageSequence#inSequenceNumber} in cache. If cache
   * capacity is exceeded, cache will be evicted
   *
   * @param messageSequence - {@link MessageSequence} with the unexpected {@link MessageSequence#inSequenceNumber}
   */
  private void putInCache(MessageSequence messageSequence) {
    if (cache.size() >= cacheCapacity) {
      log.info("Cache has reached its maximum size and will be evicted. Nothing will be added to cache.");
      cache.clear();
    } else {
      cache.put(messageSequence.getInSequenceNumber(), messageSequence);
    }
  }

  /**
   * After successful addition to the buffer this method looks for {@link MessageSequence}s with the next
   * {@link MessageSequence#inSequenceNumber} and appends them if found
   *
   * @param messageSequence - {@link MessageSequence} that was previously added
   */
  private void lookupNextSequencesInCache(MessageSequence messageSequence) {
    int nextSequenceNumber = messageSequence.getInSequenceNumber() + 1;
    if (!cache.isEmpty() && cache.containsKey(nextSequenceNumber)) {
      addMessageSequence(cache.remove(nextSequenceNumber));
    }
  }

  /**
   * Checks if sequence number is expected.
   *
   * @param messageSequence - {@link MessageSequence} to check
   * @return -- true if sequence number is expected
   */
  private boolean isSequenceNumberExpected(MessageSequence messageSequence) {
    return orderedMessageSequenceBuffer.isEmpty() ||
               messageSequence.getInSequenceNumber() < orderedMessageSequenceBuffer.last().getInSequenceNumber() ||
               orderedMessageSequenceBuffer.last().getInSequenceNumber() + 1 == messageSequence.getInSequenceNumber();
  }

  /**
   * Returns the {@link Map} that represents cache. For testing purposes
   *
   * @return - {@link Map} that represents cache
   */
  Map<Integer, MessageSequence> getCache() {
    return cache;
  }

  /**
   * Returns the backing {@link NavigableSet}. For testing purposes
   *
   * @return - the backing {@link NavigableSet}
   */
  NavigableSet<MessageSequence> getOrderedMessageSequenceBuffer() {
    return orderedMessageSequenceBuffer;
  }
}
