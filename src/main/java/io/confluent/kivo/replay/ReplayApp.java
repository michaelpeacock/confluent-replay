package io.confluent.kivo.replay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kivo.models.MyConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

//@Component
public class ReplayApp {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @PostConstruct
    private void initializeReplay() {
        Long startTime = 0L;

        Properties settings = new Properties();
        settings.put(ConsumerConfig.GROUP_ID_CONFIG, "kivo-consumer1");
        settings.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        settings.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        settings.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        settings.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        ReplayConsumer<String, String> consumer = new ReplayConsumer<>(settings, startTime);
        Collection<String> topicList = new ArrayList<>();
        topicList.add("splunk-s2s-events");

        consumer.subscribe(topicList);

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
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }

                /*
                Timestamp timestamp = new Timestamp(record.timestamp());
                System.out.printf("partition = %d, timestamp = %s, offset = %d, key = %s, value = %s\n",
                        record.partition(), sdf.format(timestamp), record.offset(), record.key(), record.value());
                */
            }
        }
    }
}
