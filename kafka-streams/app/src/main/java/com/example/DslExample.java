package com.example;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;


import java.time.Duration;
import java.util.Properties;

public class DslExample {

    public static void main(String[] args) {
        // the builder is used to construct the topology
        StreamsBuilder builder = new StreamsBuilder();

        // read from the source topic, "orders-topic"
        KStream<Void, String> stream = builder.stream("orders-topic");

        // extract product from each order
        KStream<String, String> productStream = stream
                .map((key, value) -> KeyValue.pair(getProductFromOrder(value), value));

        // create a tumbling window of 1 minute
        TimeWindows tumblingWindow = TimeWindows.of(Duration.ofMinutes(1));

        // count the number of orders per product within the tumbling window
        KTable<Windowed<String>, Long> orderCountPerProduct = productStream
                .groupByKey()
                .windowedBy(tumblingWindow)
                .count();

        // modify the output message to include the window start and end timestamps
        KStream<String, String> outputStream = orderCountPerProduct
                .toStream()
                .map((windowedKey, count) -> {
                    String product = windowedKey.key();
                    long windowStart = windowedKey.window().start();
                    long windowEnd = windowedKey.window().end();
                    String message = "(DSL) Product: " + product + ", Orders: " + count
                            + ", Window Start: " + windowStart + ", Window End: " + windowEnd;
                    return KeyValue.pair(product, message);
                });

        // send the modified output message to a new Kafka topic
        String outputTopic = "aggregations-topic";
        outputStream.foreach(
                (key, value) -> {
                    // Create a Kafka producer record with the key, value, and target topic
                    ProducerRecord<String, String> record = new ProducerRecord<>(outputTopic, key, value);

                    // Send the record to the Kafka topic
                    KafkaProducer<String, String> producer = createProducer();
                    producer.send(record);
                    producer.close();
                }
        );

        // set the required properties for running Kafka Streams
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "dev1");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:29092");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        // build the topology and start streaming
        KafkaStreams streams = new KafkaStreams(builder.build(), config);
        streams.start();

        // close Kafka Streams when the JVM shuts down (e.g. SIGTERM)
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    private static String getProductFromOrder(String order) {
        // Assuming the order format is "<product>,<other-fields>"
        String[] fields = order.split(",");
        return fields[0];
    }

    private static KafkaProducer<String, String> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:29092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        return new KafkaProducer<>(props);
    }
}

