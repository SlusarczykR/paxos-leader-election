package com.slusarczykr.paxos.leader.election.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConstructorBinding
@ConfigurationProperties(prefix = "paxos.server.leader-election")
@Data
public class LeaderElectionProperties {

    private final int heartbeatsInterval;
}
