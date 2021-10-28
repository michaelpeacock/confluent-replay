package io.confluent.kivo.replay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kivo.config.AppProperties;
import io.confluent.kivo.controllers.ReplayDataController;
import io.confluent.kivo.models.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

@Component
public class ReplayManager {
    @Autowired
    private AppProperties prop;

    @Autowired
    ReplayDataController replaySender;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private ReplayConsumer<String, String> consumer;
    private ObjectMapper objectMapper = new ObjectMapper();
    private String timeAttribute = null;

    @PostConstruct
    private void initializeReplay() {
        Properties settings = prop.getProperties();
        settings.put(ConsumerConfig.GROUP_ID_CONFIG, "kivo-consumer");
        settings.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        settings.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        settings.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        consumer = new ReplayConsumer<>(settings);
        this.timeAttribute = "timePosition";
        consumer.setTimeAttribute(this.timeAttribute);

        start();
    }

    public void setConfig(ReplayConfig config) { consumer.setConfig(config); }

    public void setSpeed(Integer speed) {
        consumer.setSpeed(speed);
    }

    public void setState(String state) { consumer.setState(state); }

    public void setTime(Long time) { consumer.setReplayTime(time); }

    private void start() {
        Thread thread = new Thread(() -> {
            while(true) {
                ReplayDataList replayDataList = new ReplayDataList();
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                if (!records.isEmpty()) {
                    for (ConsumerRecord<String, String> record : records) {
                        Long recordTime = record.timestamp();
                        if (this.timeAttribute != null) {
                            try {
                                JsonNode jsonNode = objectMapper.readTree(record.value());
                                if (jsonNode.has(timeAttribute)) {
                                    recordTime = jsonNode.get(timeAttribute).asLong();
                                }
                            } catch (JsonProcessingException e) {
                                e.printStackTrace();
                            }
                        }

                        ReplayData data = new ReplayData();
                        data.setTimestamp(recordTime);
                        data.setKey(record.key());
                        data.setValue(record.value());

                        replayDataList.addReplayData(data);
                    }
                } else if (consumer.getReplayState().matches("PLAY")) {
                    ReplayData data = new ReplayData();
                    data.setTimestamp(consumer.getReplayTime());

                    replayDataList.addReplayData(data);
                }

                if (!replayDataList.getReplayDataList().isEmpty()) {
                    replaySender.sendReplayData(replayDataList);
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }
}
