package io.confluent.kivo.test;

public class DataRecord {

    Long count;

    public DataRecord() {
    }

    public DataRecord(Long count) {
        this.count = count;
    }

    public Long getCount() {
        return count;
    }
}
