package com.slusarczykr.paxos.leader.discovery.state;

import com.slusarczykr.paxos.leader.discovery.config.ServerDiscoveryConfiguration;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Data
public class ServerDetails {

    private static final Logger log = LoggerFactory.getLogger(ServerDetails.class);

    private final AtomicInteger id = new AtomicInteger(0);
    private final AtomicLong term = new AtomicLong(0);
    private final AtomicLong commitIndex = new AtomicLong(0);

    private final AtomicBoolean leader = new AtomicBoolean(false);

    @Value("${server.port}")
    private int serverPort;

    private final ServerDiscoveryConfiguration serverDiscoveryConfiguration;

    public ServerDetails(ServerDiscoveryConfiguration serverDiscoveryConfiguration) {
        this.serverDiscoveryConfiguration = serverDiscoveryConfiguration;
    }

    @PostConstruct
    public void init() {
        initServerDetails();
    }

    private void initServerDetails() {
        int serverId = calculateServerId(serverPort);
        id.set(serverId);
        incrementTerm();
    }

    public void incrementTerm() {
        long nextTerm = calculateNextTerm();
        log.info("New term: {}", nextTerm);
        term.set(nextTerm);
    }

    private long calculateNextTerm() {
        long currentTerm = getTermValue();
        currentTerm++;

        while (currentTerm % countServers() != getIdValue()) {
            currentTerm++;
        }
        return currentTerm;
    }

    public int calculateServerId(int port) {
        return port % countServers();
    }

    private int countServers() {
        return serverDiscoveryConfiguration.getHosts().size();
    }

    public long getIdValue() {
        return getId().get();
    }

    public long getTermValue() {
        return getTerm().get();
    }

    public long getCommitIndexValue() {
        return getCommitIndex().get();
    }

    public boolean isLeader() {
        return getLeader().get();
    }

    public void setLeader(boolean leader) {
        getLeader().set(leader);
    }
}
