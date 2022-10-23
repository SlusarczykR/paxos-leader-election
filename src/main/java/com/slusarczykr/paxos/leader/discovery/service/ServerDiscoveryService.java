package com.slusarczykr.paxos.leader.discovery.service;

import java.util.Map;

public interface ServerDiscoveryService {

    Map<Integer, String> allServers();

    Map<Integer, String> getServers();

    int getNumberOfAvailableServers();

    String getServerLocation(int id);
}
