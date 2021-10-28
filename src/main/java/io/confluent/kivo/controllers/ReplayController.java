package io.confluent.kivo.controllers;

import io.confluent.kivo.models.ReplayConfig;
import io.confluent.kivo.replay.ReplayManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReplayController {
    @Autowired
    ReplayManager replay;

    @PostMapping({"setConfigValues"})
    public void setConfigValues(@RequestBody ReplayConfig setup) {
        System.out.println("setConfigValues: " + setup.toString());
        replay.setConfig(setup);
    }

    @PostMapping({"setSpeed"})
    public void setSpeed(@RequestBody Integer speed) {
        System.out.println("setSpeed: " + speed);
        replay.setSpeed(speed);
    }

    @PostMapping({"setState"})
    public void setState(@RequestBody String state) {
        System.out.println("setState: " + state);
        replay.setState(state);
    }

    @PostMapping({"setTime"})
    public void setState(@RequestBody Long time) {
        System.out.println("setTime: " + time);
        replay.setTime(time);
    }

}
