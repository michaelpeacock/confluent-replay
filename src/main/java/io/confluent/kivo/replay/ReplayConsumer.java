package io.confluent.kivo.replay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kivo.models.ReplayConfig;
import io.confluent.kivo.models.ReplayState;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.*;

public class ReplayConsumer<K, V> implements ConsumerRebalanceListener {
    private long replayTime = 0;

    private ReplayConfig config = new ReplayConfig();
    private ReplayState state = new ReplayState();

    private KafkaConsumer<K, V> consumer;
    private ConsumerRebalanceListener clientListener = null;
    private List<ConsumerRecord<K, V>> recordsCache = new ArrayList<>();
    private Boolean partitionsAssigned = false;
    private String timeAttribute = null;
    private ObjectMapper objectMapper = new ObjectMapper();


    public ReplayConsumer(Properties properties) {
        consumer = new KafkaConsumer<K,V>(properties);
    }

    public void setConfig(ReplayConfig config) {
        this.config = config;
        setReplayTime(config.getStartTime());
     }

    public void setSpeed(Integer speed) {
        this.state.setReplaySpeed(speed);
    }

    public void setState(String state) {
        this.state.setReplayState(state);
    }

    public String getReplayState() {
        return this.state.getReplayState();
    }

    public void setTimeAttribute(String timeAttribute) {
        this.timeAttribute = timeAttribute;
    }

    public void setReplayTime(Long time) {
        this.replayTime = time;

        if (partitionsAssigned == false) {
            Collection<String> topicList = new ArrayList<>();
            topicList.add(config.getTopic());
            consumer.subscribe(topicList, this);
        } else {
            recordsCache.clear();
            offsetToTimeStamp();
        }
    }

    public Long getReplayTime() {
        return replayTime;
    }

    public ConsumerRecords<K, V> poll(Duration timeout) {
        // if the cache is empty, poll for more
        Map<TopicPartition, List<ConsumerRecord<K, V>>> returnRecords = new HashMap<>();
        List<ConsumerRecord<K, V>> returnList = new ArrayList<>();
        if (state.getReplayState().matches("PLAY")) {
            replayTime += state.getReplaySpeed() * (timeout.toMillis());
            System.out.println("ReplayConsumer polling - replayTime: " + replayTime +
                    " timeout: " + timeout.toMillis() +
                    " speed: " + state.getReplaySpeed()
            );

            if (recordsCache.isEmpty()) {
                ConsumerRecords<K, V> results = consumer.poll(timeout);

                results.forEach(record -> {
                    Long recordTime = record.timestamp();
                    if (timeAttribute != null) {
                        try {
                            JsonNode jsonNode = objectMapper.readTree(record.value().toString());
                            if (jsonNode.has(timeAttribute)) {
                                recordTime = jsonNode.get(timeAttribute).asLong();
                            }

                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    }

                    if (recordTime < replayTime) {
                        returnList.add(record);
                    } else {
                        recordsCache.add(record);
                    }
                });
            }

            for (Iterator<ConsumerRecord<K, V>> iterator = recordsCache.iterator(); iterator.hasNext();) {
                ConsumerRecord<K, V> record = iterator.next();
                //System.out.println("cache replay time: " + replayTime + " record time: " + record.timestamp());
                Long recordTime = record.timestamp();
                if (timeAttribute != null) {
                    try {
                        JsonNode jsonNode = objectMapper.readTree(record.value().toString());
                        if (jsonNode.has(timeAttribute)) {
                            recordTime = jsonNode.get(timeAttribute).asLong();
                        }
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }

                if (recordTime < replayTime) {
                    returnList.add(record);
                    iterator.remove();
                }
            }

            if (!returnList.isEmpty()) {
                returnRecords.put(new TopicPartition(config.getTopic(), 0), returnList);
            }
        }

        return new ConsumerRecords<>(returnRecords);
    }

    public void offsetToTimeStamp() {
        // if the start time wasn't set, go to the first offset
        if (config.getStartTime() != 0L) {
            Map<TopicPartition, Long> timestampsToSearch = new HashMap<>();

            Set<TopicPartition> partitions = consumer.assignment();
            partitions.forEach(tp -> timestampsToSearch.computeIfAbsent(tp, tp1 -> config.getStartTime()));
            Map<TopicPartition, OffsetAndTimestamp> offsets = consumer.offsetsForTimes(timestampsToSearch);
            offsets.forEach((k, v) -> {
                if (v != null) {
                    consumer.seek(k, v.offset());
                }});
        } else {
            consumer.seekToBeginning(consumer.assignment());
        }
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        if (clientListener != null) {
            clientListener.onPartitionsRevoked(partitions);
        }
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        System.out.println("partitions assigned");
        partitionsAssigned = true;
        offsetToTimeStamp();

        if (clientListener != null) {
            clientListener.onPartitionsAssigned(partitions);
        }
    }

    @Override
    public void onPartitionsLost(Collection<TopicPartition> partitions) {
        if (clientListener != null) {
            clientListener.onPartitionsLost(partitions);
        }
    }
}
