package io.confluent.kivo.models;

import java.util.ArrayList;
import java.util.List;

public class ReplayDataList {
    private List<ReplayData> replayDataList = new ArrayList<>();

    public List<ReplayData> getReplayDataList() {
        return replayDataList;
    }

    public void setReplayDataList(List<ReplayData> replayDataList) {
        this.replayDataList = replayDataList;
    }

    public void addReplayData(ReplayData data) {
        this.replayDataList.add(data);
    }

}
