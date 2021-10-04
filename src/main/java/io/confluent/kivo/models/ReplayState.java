package io.confluent.kivo.models;

import java.util.ArrayList;
import java.util.List;

public class ReplayState {
    private Double replayTime;
    private Integer replaySpeed = 1;
    private String replayState = "STOP"; // PLAY, PAUSE, STOP
    private List<ReplayData> replayData = new ArrayList<>();

    public Double getReplayTime() {
        return replayTime;
    }

    public void setReplayTime(Double replayTime) {
        this.replayTime = replayTime;
    }

    public Integer getReplaySpeed() {
        return replaySpeed;
    }

    public void setReplaySpeed(Integer replaySpeed) {
        this.replaySpeed = replaySpeed;
    }

    public String getReplayState() {
        return replayState;
    }

    public void setReplayState(String replayState) {
        this.replayState = replayState;
    }

    public List<ReplayData> getReplayData() {
        return replayData;
    }

    public void setReplayData(List<ReplayData> replayData) {
        this.replayData = replayData;
    }

    @Override
    public String toString() {
        return "ReplayState{" +
                "replayTime=" + replayTime +
                ", replaySpeed=" + replaySpeed +
                ", replayState='" + replayState + '\'' +
                ", replayData=" + replayData +
                '}';
    }
}
