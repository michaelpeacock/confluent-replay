package io.confluent.kivo.replay;

import io.confluent.kivo.config.AppProperties;
import io.confluent.kivo.controllers.ReplayDataController;
import io.confluent.kivo.models.ReplayConfig;
import io.confluent.kivo.models.ReplayData;
import io.confluent.kivo.models.ReplayState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

//@Component
public class SimpleReplayConsumer {
    @Autowired
    ReplayDataController replaySender;

    private Long replayTime = 0L;
    private ReplayConfig config = new ReplayConfig();
    private ReplayState state = new ReplayState();
    private Boolean running = false;

    public void setConfig(ReplayConfig config) {
        this.config = config;
        this.replayTime = config.getStartTime();
    }

    public void setSpeed(Integer speed) {
        this.state.setReplaySpeed(speed);
    }

    public void setState(String state) {
        this.state.setReplayState(state);

        if (state.matches("PLAY") && running == false) {
            start();
        }
    }

    public void setTime(Long time) {
        this.replayTime = time;
    }

    private void start() {
        long sleepTime = 100;
        int count = 0;
        running = true;
        Thread thread = new Thread(() -> {
            while(true) {
               Long interval =  state.getReplaySpeed() * (sleepTime / 1000);
               if (state.getReplayState().matches("PLAY"))  {
                   this.replayTime += interval;

                   ReplayData data = new ReplayData();
                   data.setTimestamp(this.replayTime);
                   data.setKey("test-key-" + this.replayTime);
                   data.setValue("test-value-" + this.replayTime);

                   System.out.println("sending replay data - replayTime: " + this.replayTime);
                   //replaySender.sendReplayData(data);
               }

                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }
}
