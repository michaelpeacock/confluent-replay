package io.confluent.kivo.controllers;

import io.confluent.kivo.models.ReplayData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ReplayDataController {
    @Autowired
    private SimpMessagingTemplate template;

    public void sendReplayData(ReplayData data) {
        this.template.convertAndSend("/topic/replay-data", data);
    }

}
