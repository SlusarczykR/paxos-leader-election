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

import static com.slusarczykr.paxos.leader.discovery.state.ErrorStatus.Type.INFINITE_REPLIES;
import static com.slusarczykr.paxos.leader.discovery.state.ErrorStatus.Type.INVALID_RESPONSE;
import static com.slusarczykr.paxos.leader.discovery.state.ErrorStatus.Type.LOST_CONNECTION;

@Component
@Data
public class PaxosServer {

    private static final Logger log = LoggerFactory.getLogger(PaxosServer.class);

    private final AtomicInteger id = new AtomicInteger(0);
    private final AtomicLong term = new AtomicLong(0);
    private final AtomicLong commitIndex = new AtomicLong(0);

    private final AtomicBoolean leader = new AtomicBoolean(false);

    private final ErrorStatus errorStatus = new ErrorStatus();

    @Value("${server.port}")
    private int serverPort;

    private final ServerDiscoveryConfiguration serverDiscoveryConfiguration;

    public PaxosServer(ServerDiscoveryConfiguration serverDiscoveryConfiguration) {
        this.serverDiscoveryConfiguration = serverDiscoveryConfiguration;
    }

    @PostConstruct
    public void init() {
        initPaxosServer();
    }

    private void initPaxosServer() {
        initServerId();
        incrementTerm();
    }

    private void initServerId() {
        int serverId = calculateServerId(serverPort);
        id.set(serverId);
        log.info("Id {} has been assigned to the server", serverId);
    }

    public void incrementTerm() {
        long nextTerm = calculateNextTerm();
        log.info("New term: {}", nextTerm);
        updateTerm(nextTerm);
    }

    public void updateTerm(long term) {
        this.term.set(term);
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

    public boolean isInvalidResponseEnabled() {
        return errorStatus.isEnabled(INVALID_RESPONSE);
    }

    public boolean isInfiniteRepliesEnabled() {
        return errorStatus.isEnabled(INFINITE_REPLIES);
    }

    public boolean isLostConnectionEnabled() {
        return errorStatus.isEnabled(LOST_CONNECTION);
    }

    public void enableError(ErrorStatus.Type errorType) {
        log.debug("Enabling '{}' error", errorType);
        errorStatus.enable(errorType);
    }

    public void disableError(ErrorStatus.Type errorType) {
        log.debug("Disabling '{}' error", errorType);
        errorStatus.disable(errorType);
    }
}
