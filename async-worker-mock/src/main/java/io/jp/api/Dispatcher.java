package io.jp.api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.Executors;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dispatcher extends AbstractVerticle {

  private static Logger LOG = LoggerFactory.getLogger(Dispatcher.class);

  private Producer<String, String> producer;
  private Consumer<String, String> consumer;

  private boolean running = false;

  private String topic_in = "async_request";
  private String topic_out = "async_response";

  @Override
  public void start() throws Exception {
    initProducer();
    initConsumer();
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
    Executors.newSingleThreadExecutor().submit(this::poll);
  }

  private void poll() {
    running = true;
    while (running) {
      ConsumerRecords<String, String> records = consumer.poll(1000);
      records.forEach(record -> {
        LOG.info("Dispatch msg=[{}] from topic {} to topic {}",
            record.value(),
            topic_in,
            topic_out);
        new JsonObject(record.value());
        producer.send(new ProducerRecord<String, String>(topic_out, "foobar", record.value()));
      });
    }
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

  @Override
  public void stop() throws Exception {
    producer.close();
    running = false;
  }
}
