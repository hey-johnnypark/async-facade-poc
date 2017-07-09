package io.jp.api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class Dispatcher extends AbstractVerticle {

  private static Logger LOG = LoggerFactory.getLogger(Dispatcher.class);

  private Map httpApiMap;

  private Producer<String, String> producer;
  private Consumer<String, String> consumer;

  private boolean running = false;

  private String topic_out = "async_request";
  private String topic_in = "async_response";

  private String eb_local_address;

  @Override
  public void start() throws Exception {
    initProducer();
    initConsumer();
    initSyncAddress();
    vertx.eventBus().localConsumer(Constants.EB_ADDRESS_DISPATCHER).handler(this::dispatch);
  }

  private void initSyncAddress() {
    eb_local_address = "SYNC_" + UUID.randomUUID().toString();
    vertx.eventBus().consumer(eb_local_address).handler(this::sendUpstream);
  }

  private void sendUpstream(Message<Object> obj) {
    vertx.eventBus().send(Constants.EB_ADDRESS_API, obj.body());
  }

  private void initConsumer() {
    Properties props = new Properties();
    props.put("bootstrap.servers", "localhost:9092");
    props.put("group.id", "test");
    props.put("enable.auto.commit", "true");
    props.put("auto.commit.interval.ms", "1000");
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    consumer = new KafkaConsumer<>(props);
    consumer.subscribe(Arrays.asList(topic_in));
    Executors.newSingleThreadExecutor().submit(this::consume);
  }

  private void consume() {
    running = true;
    while (running) {
      consumer.poll(500).forEach(this::handleConsumedRecord);
    }
  }

  private void handleConsumedRecord(ConsumerRecord<String, String> record) {
    LOG.info("Received {} from {}", record.value(), topic_in);
    JsonObject obj = new JsonObject(record.value());

    vertx.sharedData().getClusterWideMap("sync", res -> {
      res.result().remove(obj.getString("id"), assoc -> {
        if (assoc.succeeded()) {
          LOG.info("Found assoc {} -> {}", obj.getString("id"), assoc.result());
          vertx.eventBus().send((String) assoc.result(), obj);
        }
      });
    });
  }

  private void initProducer() {
    Properties props = new Properties();
    props.put("bootstrap.servers", "localhost:9092");
    props.put("acks", "all");
    props.put("retries", 0);
    props.put("batch.size", 16384);
    props.put("linger.ms", 1);
    props.put("buffer.memory", 33554432);
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    producer = new KafkaProducer<String, String>(props);
  }

  private void dispatch(Message<Object> request) {
    JsonObject obj = ((JsonObject) request.body());
    LOG.info("Send msg=[{}] to {}", obj, topic_out);
    vertx.sharedData().getClusterWideMap("sync", res -> {
      res.result().put(obj.getString("id"), eb_local_address, putSuccess -> {
        LOG.info("Assigned {} to {}", obj.getString("id"), eb_local_address);
        producer.send(new ProducerRecord<String, String>(topic_out, "foobar", obj.toString()));
      });
    });
  }

  @Override
  public void stop() throws Exception {
    producer.close();
    running = false;
  }
}
