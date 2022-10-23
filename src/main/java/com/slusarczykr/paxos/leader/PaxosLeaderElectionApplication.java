package com.slusarczykr.paxos.leader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PaxosLeaderElectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaxosLeaderElectionApplication.class, args);
    }
}
