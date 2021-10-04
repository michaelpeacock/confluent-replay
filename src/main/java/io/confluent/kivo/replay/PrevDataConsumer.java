package io.confluent.kivo.replay;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.*;

public class PrevDataConsumer {
    private KafkaConsumer<String, String> consumer;
    private Collection<String> topicList = new ArrayList<>();
    private Boolean partitionsAssigned = false;
    private long startTime = 0L;
    private Boolean offsetTimeSet = true;

    public PrevDataConsumer() {
       initialize();
    }

    public void initialize() {
        Properties settings = new Properties();
        settings.put(ConsumerConfig.GROUP_ID_CONFIG, "kivo-consumer");
        settings.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        settings.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        settings.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        settings.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        consumer = new KafkaConsumer<>(settings);
        topicList.add("flights_raw");

        ConsumerRebalanceListener listener = new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                // nothing to do...
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                System.out.println("partitions assigned");
                partitionsAssigned = true;
            }
        };

        consumer.subscribe(topicList, listener);
    }

    public void start() {
        try {

            while (true) {
                if (partitionsAssigned && !offsetTimeSet) {
                    offsetToTimeStamp();
                }

                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    System.out.printf("partition = %d, timestamp = %d, offset = %d, key = %s, value = %s\n",
                            record.partition(), record.timestamp(), record.offset(), record.key(), record.value());

                    if (record.offset() == 1629) {
                        setTimeStamp(1625786397309L);
                    }
                }


            }
        }
        finally{
            System.out.println("*** Ending Consumer ***");
            consumer.close();
        }

    }

    public void setTimeStamp(long startTime) {
        this.startTime = startTime;
        offsetTimeSet = false;
    }

    public void offsetToTimeStamp() {
        System.out.println("in setTimeStamp");
        for (String topic : topicList) {
            Map<TopicPartition, Long> timestampsToSearch = new HashMap<>();
            //final long tenSecondsAgo = 1625786392574L; //System.currentTimeMillis() - 10_000L;

            Set<TopicPartition> partitions = consumer.assignment();
            partitions.forEach(tp -> timestampsToSearch.computeIfAbsent(tp, tp1 -> startTime));
            Map<TopicPartition, OffsetAndTimestamp> offsets = consumer.offsetsForTimes(timestampsToSearch);
            offsets.forEach((k, v) -> consumer.seek(k, v.offset()));
        }

        offsetTimeSet = true;
    }

    public static void main(String[] args) {
        PrevDataConsumer consumer = new PrevDataConsumer();

        consumer.setTimeStamp(1625786392574L);

        consumer.start();


        /*
        ConsumerRebalanceListener listener = new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                // nothing to do...
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                //1625786392574

                Map<TopicPartition, Long> timestampsToSearch = new HashMap<>();
                final long tenSecondsAgo = 1625786392574L; //System.currentTimeMillis() - 10_000L;
                partitions.forEach(tp -> timestampsToSearch.computeIfAbsent(tp, tp1 -> tenSecondsAgo));
                Map <TopicPartition,OffsetAndTimestamp> offsets = consumer.offsetsForTimes(timestampsToSearch);
                offsets.forEach((k, v) -> consumer.seek(k, v.offset()));

                //consumer.seekToBeginning(partitions);
            }
        };
        */
    }
}