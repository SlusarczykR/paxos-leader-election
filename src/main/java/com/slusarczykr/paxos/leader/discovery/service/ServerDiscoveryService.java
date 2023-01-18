package com.slusarczykr.paxos.leader.discovery.service;

import java.util.Map;

public interface ServerDiscoveryService {

    Map<Integer, String> getAllServers();

    Map<Integer, String> getAvailableServers();

    int getNumberOfAvailableServers();

    boolean anyServerAvailable();

    String getServerLocation(int id);
}
