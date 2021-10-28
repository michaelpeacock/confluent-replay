package io.confluent.kivo.models;

public class ReplayData {
    private Long timestamp;
    private String key;
    private String value;

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "ReplayData{" +
                "timestamp=" + timestamp +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
