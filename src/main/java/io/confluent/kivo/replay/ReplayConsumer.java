package io.confluent.kivo.replay;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

public class ReplayConsumer<K, V> extends KafkaConsumer<K, V> implements ConsumerRebalanceListener {
    private long startTime = 0;  // reset to start time
    private long replayTime = 0;
    private long previousTime = 0;
    private double replaySpeed = 1.0;
    private ConsumerRebalanceListener clientListener = null;
    private Map<TopicPartition, List<ConsumerRecord<K, V>>> recordsCache = new HashMap<>();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public ReplayConsumer(Map<String, Object> configs, Long startTime) {
        super(configs);

        this.startTime = startTime;
        this.replayTime = this.startTime;
    }

    public ReplayConsumer(Properties properties, Long startTime) {
        super(properties);

        this.startTime = startTime;
        this.replayTime = this.startTime;
    }

    public ReplayConsumer(Properties properties, Deserializer<K> keyDeserializer, Deserializer<V> valueDeserializer, Long startTime) {
        super(properties, keyDeserializer, valueDeserializer);

        this.startTime = startTime;
        this.replayTime = this.startTime;
    }

    public ReplayConsumer(Map<String, Object> configs, Deserializer<K> keyDeserializer, Deserializer<V> valueDeserializer, Long startTime) {
        super(configs, keyDeserializer, valueDeserializer);

        this.startTime = startTime;
        this.replayTime = this.startTime;
    }

    @Override
    public ConsumerRecords<K, V> poll(Duration timeout) {
        // if the cache is empty, poll for more
        if (recordsCache.isEmpty()) {
            ConsumerRecords<K, V> results = super.poll(timeout);
            System.out.println("polling - results: " + results.count());
            results.iterator();

            Set<TopicPartition> partitions = results.partitions();
            for (TopicPartition partition : partitions) {
                System.out.println("Partition: " + partition + " Results: " + results.count());
                List<ConsumerRecord<K, V>> recordResults = new ArrayList<>(results.records(partition));
                recordsCache.put(partition, recordResults);

                // if no start time was supplied, set it to the start time of the
                // the first record
                if (startTime == 0L) {
                    startTime = results.records(partition).get(0).timestamp();
                    replayTime = startTime;

                    Timestamp timestamp = new Timestamp(replayTime);
                    System.out.println("initializing replayTime " + sdf.format(timestamp));
                }
            }
        } else {
            try {
                Thread.sleep(timeout.toMillis());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Long currentTime = System.currentTimeMillis();
        if (previousTime == 0L) {
            previousTime = currentTime;
        }

        replaySpeed = 10.0;
        long deltaTime = currentTime - previousTime;
        replayTime += deltaTime * replaySpeed;
        previousTime = currentTime;
        Timestamp timestamp = new Timestamp(replayTime);
        System.out.println("Replay Time: " + sdf.format(timestamp));

        Map<TopicPartition, List<ConsumerRecord<K, V>>> returnRecords = new HashMap<>();
        Iterator<Map.Entry<TopicPartition, List<ConsumerRecord<K, V>>>> iterator = recordsCache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<TopicPartition, List<ConsumerRecord<K, V>>> entry = iterator.next();
            List<ConsumerRecord<K, V>> replayList = new ArrayList<>();
            ListIterator<ConsumerRecord<K, V>> recordIter = entry.getValue().listIterator();
            while(recordIter.hasNext ()) {
                ConsumerRecord<K, V> record = recordIter.next();
                //System.out.println("replay time: " + replayTime + " record time: " + record.timestamp());
                if (record.timestamp() < replayTime) {
                    replayList.add(record);
                    recordIter.remove();

                    if (entry.getValue().isEmpty()) {
                        iterator.remove();
                    }
                }
            }

            if (!replayList.isEmpty()) {
                returnRecords.put(entry.getKey(), replayList);
            }
        }

        return new ConsumerRecords<>(returnRecords);
    }

    public void offsetToTimeStamp() {
        System.out.println("in setTimeStamp\n");

        // if the start time wasn't set, go to the first offset
        if (startTime != 0L) {
            Map<TopicPartition, Long> timestampsToSearch = new HashMap<>();

            Set<TopicPartition> partitions = assignment();
            partitions.forEach(tp -> timestampsToSearch.computeIfAbsent(tp, tp1 -> startTime));
            Map<TopicPartition, OffsetAndTimestamp> offsets = offsetsForTimes(timestampsToSearch);
            offsets.forEach((k, v) -> seek(k, v.offset()));
        } else {
            this.seekToBeginning(assignment());
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

    @Override
    public void subscribe(Collection<String> topics, ConsumerRebalanceListener listener) {
        this.clientListener = listener;

        super.subscribe(topics, this);
    }

    @Override
    public void subscribe(Collection<String> topics) {
        super.subscribe(topics, this);
    }

    @Override
    public void subscribe(Pattern pattern, ConsumerRebalanceListener listener) {
        this.clientListener = listener;

        super.subscribe(pattern, this);
    }

    @Override
    public void subscribe(Pattern pattern) {
        super.subscribe(pattern, this);
    }


}
