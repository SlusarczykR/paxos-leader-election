package com.slusarczykr.paxos.leader.discovery.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "paxos.server.discovery")
@Getter
@Setter
public class ServerDiscoveryConfiguration {

    private List<String> hosts;
}
