package com.slusarczykr.paxos.leader.election.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConstructorBinding
@ConfigurationProperties(prefix = "paxos.server.leader-election")
@Data
public class LeaderElectionProperties {

    public static final int DEFAULT_MIN_AWAIT_TIME = 15;
    public static final int DEFAULT_MAX_AWAIT_TIME = 30;
    public static final int DEFAULT_HEARTBEATS_INTERVAL = 5;

    private int minAwaitTime = DEFAULT_MIN_AWAIT_TIME;
    private int maxAwaitTime = DEFAULT_MAX_AWAIT_TIME;
    private int heartbeatsInterval = DEFAULT_HEARTBEATS_INTERVAL;

    public void reset() {
        this.minAwaitTime = DEFAULT_MIN_AWAIT_TIME;
        this.maxAwaitTime = DEFAULT_MAX_AWAIT_TIME;
        this.heartbeatsInterval = DEFAULT_HEARTBEATS_INTERVAL;
    }
}
