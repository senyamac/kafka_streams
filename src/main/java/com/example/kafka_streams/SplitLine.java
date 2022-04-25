package com.example.kafka_streams;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;

public class SplitLine {
  public static final String SERVER="localhost:9092";
  public static final String ID_CONFIG="streams-linesplit";
  public static final String INPUT_TOPIC="streams-plaintext-input";
  public static final String OUTPUT_TOPIC="streams-linesplit-output";

  static Properties getBuilderProperties() {
    Properties props = new Properties();
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, ID_CONFIG);
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, SERVER);
    props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
    props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

    return props;
  }

  static void setStream(StreamsBuilder builder) {
    builder.<String, String>stream(INPUT_TOPIC)
        .flatMapValues(value -> Arrays.asList(value.split("\\W+")))
        .to(OUTPUT_TOPIC);
  }

  public static void main(String[] args) {
    Properties props = getBuilderProperties();
    final StreamsBuilder builder = new StreamsBuilder();
    setStream(builder);

    final Topology topology = builder.build();
    final KafkaStreams streams = new KafkaStreams(topology, props);
    final CountDownLatch latch = new CountDownLatch(1);

    // attach shutdown handler to catch control-c
    Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
      @Override
      public void run() {
        streams.close();
        latch.countDown();
      }
    });

    try {
      streams.start();
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      System.exit(1);
    }
    System.exit(0);
  }
}
