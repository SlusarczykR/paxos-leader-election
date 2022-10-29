package com.slusarczykr.paxos.leader.discovery.service;

import com.slusarczykr.paxos.leader.discovery.config.ServerDiscoveryConfiguration;
import com.slusarczykr.paxos.leader.discovery.state.ServerDetails;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServerDiscoveryServiceImpl implements ServerDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(ServerDiscoveryServiceImpl.class);

    private final RestTemplate webClient = new RestTemplate();
    private final ServerDetails serverDetails;
    private final ServerDiscoveryConfiguration serverDiscoveryConfiguration;

    private final Map<Integer, String> paxosServers = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("Initializing service discovery cache...");
        registerPaxosServers();
    }

    private void registerPaxosServers() {
        Optional.ofNullable(serverDiscoveryConfiguration)
                .ifPresent(this::registerPaxosServers);
    }

    private void registerPaxosServers(ServerDiscoveryConfiguration configuration) {
        configuration.getHosts().forEach(server ->
                paxosServers.put(serverDetails.calculateServerId(extractPort(server)), server)
        );
    }

    private int extractPort(String server) {
        int portStartIndex = server.lastIndexOf(":") + 1;
        return Integer.parseInt(server.substring(portStartIndex));
    }

    @Override
    public Map<Integer, String> allServers() {
        return Collections.unmodifiableMap(paxosServers);
    }

    @Override
    public Map<Integer, String> getServers() {
        return paxosServers.entrySet().stream()
                .filter(Predicate.not(this::isCurrentHost).and(this::isAlive))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private boolean isAlive(Map.Entry<Integer, String> serverEntry) {
        try {
            webClient.getForObject(serverEntry.getValue() + "/status", Void.class);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public int getNumberOfAvailableServers() {
        return countAvailableServers() + 1;
    }

    private int countAvailableServers() {
        return (int) paxosServers.entrySet().stream()
                .filter(Predicate.not(this::isCurrentHost))
                .filter(this::isAlive)
                .count();
    }

    private boolean isCurrentHost(Map.Entry<Integer, String> serverEntry) {
        return serverEntry.getKey() == serverDetails.getIdValue();
    }

    @Override
    public String getServerLocation(int id) {
        return paxosServers.get(id);
    }
}
