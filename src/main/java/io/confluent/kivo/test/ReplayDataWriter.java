package io.confluent.kivo.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kivo.models.MyConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

//@Component
public class ReplayDataWriter {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    //@PostConstruct
    public static void main(String[] args) {
    //private void initializeReplay() {
        Long startTime = 0L;

        Properties settings = new Properties();
        settings.put(ConsumerConfig.GROUP_ID_CONFIG, "kivo-consumer3");
        settings.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        settings.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        settings.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        settings.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        KafkaConsumer<String, String>consumer = new KafkaConsumer<>(settings);
        Collection<String> topicList = new ArrayList<>();
        topicList.add("splunk-s2s-events");

        consumer.subscribe(topicList);
        String fileName = "/Users/mpeacock/Development/Demos/consumer_data.out";
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, String> record : records) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    String jsonStr = null;

                    MyConsumerRecord myRecord = new MyConsumerRecord();
                    myRecord.setOffset(record.offset());
                    myRecord.setPartition(record.partition());
                    myRecord.setTimestamp(record.timestamp());
                    myRecord.setKey(record.key());
                    myRecord.setValue(record.value());

                    jsonStr = mapper.writeValueAsString(myRecord);
                    System.out.println(jsonStr);
                    writer.write(jsonStr);
                    writer.newLine();

                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }
}
