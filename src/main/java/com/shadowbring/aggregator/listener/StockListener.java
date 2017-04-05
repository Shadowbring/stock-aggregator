package com.shadowbring.aggregator.listener;

import com.shadowbring.aggregator.aggregation.PriceLevelsAggregator;
import com.shadowbring.aggregator.domain.incoming.MessageSequence;
import com.shadowbring.aggregator.domain.incoming.Order;
import com.shadowbring.aggregator.udp.UdpCompliantMessageSequenceBuffer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.NetworkInterface;

/**
 * A verticle that is deployed to the Vert.x instance and is listening to the UDP multicast socket, waiting for the
 * incoming messages.
 * Every 2 seconds (configurable via application.properties) it processes {@link MessageSequence}s accumulated in the
 * {@link UdpCompliantMessageSequenceBuffer}, extracts {@link Order}s, aggregates them and emits via UDP multicast
 * socket
 *
 * @author Dmytro Bezruk
 */
@Component
@Slf4j
public class StockListener extends AbstractVerticle {

  /**
   * IP-address of the multicast group. Configurable via application.properties file
   */
  @Value("${multicast.address}")
  private String multicastAddress;

  /**
   * Port where the socket is opened. Configurable via application.properties file
   */
  @Value("${multicast.port}")
  private Integer port;
  /**
   * IP-address where the socket is opened. Configurable via application.properties file
   */
  @Value("${socket.host}")
  private String socketHost;

  /**
   * Interval between emissions of the aggregated price levels
   */
  @Value("${emission.period}")
  private Long emissionPeriod;

  /**
   * Multicast address for sending aggregated price levels. Configurable via application.properties file
   */
  @Value("${emission.address}")
  private String emissionAddress;

  /**
   * Port for sending aggregated price levels. Configurable via application.properties file
   */
  @Value("${emission.port}")
  private Integer emissionPort;

  /**
   * Buffer that is responsible for the reconstruction of the messages' order
   */
  @Autowired
  private UdpCompliantMessageSequenceBuffer buffer;

  /**
   * Service that performs aggregation of the price levels
   */
  @Autowired
  private PriceLevelsAggregator aggregator;

  /**
   * Verticle startup method. Opens UDP socket at the specified host and port that will then join to the multicast group
   * and wait for the incoming messages. Also registers timer that will fire every 2 seconds (configurable), perform
   * aggregation and transmit it via UDP
   *
   * @throws Exception if verticle startup vas failed
   */
  @Override
  public void start() throws Exception {
    DatagramSocket socket = vertx.createDatagramSocket(new DatagramSocketOptions()
                                                           .setReceiveBufferSize(Integer.MAX_VALUE)
                                                           .setLogActivity(true));
    String networkInterfaceName = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getName();
    socket.listen(port, socketHost, asyncResult -> {
      if (asyncResult.succeeded()) {
        registerIncomingMessageHandler(socket);
        socket.listenMulticastGroup(multicastAddress, networkInterfaceName, null,
            listenMulticastResult -> log.debug("Is listening succeeded: '{}'", listenMulticastResult.succeeded()));
      } else {
        log.error("Failed to listen multicast group", asyncResult.cause());
      }
    });

    registerPeriodicEmitter(socket);
  }

  /**
   * Timer that will fire every 2 seconds (configurable), perform
   * aggregation and transmit it via UDP
   *
   * @param socket - {@link DatagramSocket} for data transferring
   */
  private void registerPeriodicEmitter(DatagramSocket socket) {
    vertx.setPeriodic(emissionPeriod, id -> {
      log.info("Preparation for sending aggregated price levels has been started...");
      aggregator.aggregateByPriceLevels()
          .forEach(bulk -> {
            String json = Json.encode(bulk);
            log.info("The following bulk is about to be sent: {}", json);
            socket.send(Buffer.buffer(json), emissionPort, emissionAddress,
                asyncResult -> log.info("Successfully sent? {}", asyncResult.succeeded()));
          });
    });
  }

  /**
   * Registers a handler that waits for the incoming messages, maps them from JSON to POJO-classes and adds objects to
   * the {@link UdpCompliantMessageSequenceBuffer}
   *
   * @param socket - {@link DatagramSocket} for data receiving
   */
  private void registerIncomingMessageHandler(DatagramSocket socket) {
    socket.handler(packet -> {
      MessageSequence messageSequence;
      log.info("Received packet from host '{}' and port '{}'. Data: {}",
          packet.sender().host(), packet.sender().port(), packet.data().toString());
      try {
        messageSequence = packet.data().toJsonObject().mapTo(MessageSequence.class);
        log.info("JSON message was successfully parsed!");
        buffer.addMessageSequence(messageSequence);
      } catch (DecodeException e) {
        log.error("Failed to parse JSON message.", e);
      }
    });
  }
}
