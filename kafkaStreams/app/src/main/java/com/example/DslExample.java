package com.example;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.errors.StreamsException;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import kafkaStreams.app.src.main.java.com.example.serialization.json.CustomSerdes;
import kafkaStreams.app.src.main.java.com.example.serialization.OrderAggregation;


public class DslExample {
    String inputTopic = "orders-topic";
    String outputTopic = "aggregations-topic";
    String kafkaServerURL = "127.0.0.1";
    String kafkaServerPort = "29092";

    public static void main(String[] args) throws StreamsException {


        int windowLength = 1;

        StreamsBuilder builder = new StreamsBuilder();

        KStream<Void, String> stream = builder.stream(inputTopic);
        KStream<String, String> productStream = stream
                .map((key, value) -> KeyValue.pair(getProductFromOrder(value), value));

        TimeWindows tumblingWindow = TimeWindows.of(Duration.ofMinutes(windowLength));
        KTable<Windowed<String>, Long> orderCountPerProduct = productStream
                .groupByKey()
                .windowedBy(tumblingWindow)
                .count();

        KStream<String, String> outputStream = orderCountPerProduct
                .toStream()
                .map((windowedKey, count) -> {
                    String product = windowedKey.key().split(": ")[1];
                    long windowStart = windowedKey.window().start();
                    String message = String.format("{\"product_id\": \"%s\", \"timegroup\": %s, \"orders\": %s}", product, windowStart, count );

                    return KeyValue.pair(product, message);
                });


        outputStream.foreach(
                (key, value) -> {
                    ProducerRecord<String, String> record = new ProducerRecord<String, String>(outputTopic, key, value);
                    KafkaProducer<String, String> producer = createProducer();

                    producer.send(record);
                    producer.close();
                }
        );

        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "dev1");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, String.format("%s:%s", kafkaServerURL, kafkaServerPort));
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        KafkaStreams streams = new KafkaStreams(builder.build(), config);
        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    private static String getProductFromOrder(String order) {
        String[] fields = order.split(",");
        return fields[0];
    }

    private static KafkaProducer<String, String> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, String.format("%s:%s", kafkaServerURL, kafkaServerPort));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        return new KafkaProducer<String, String>(props);
    }
}

