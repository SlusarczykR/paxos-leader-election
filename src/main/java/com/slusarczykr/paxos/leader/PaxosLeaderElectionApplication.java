package com.slusarczykr.paxos.leader;

import com.slusarczykr.paxos.leader.election.config.LeaderElectionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(LeaderElectionProperties.class)
public class PaxosLeaderElectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaxosLeaderElectionApplication.class, args);
    }
}
