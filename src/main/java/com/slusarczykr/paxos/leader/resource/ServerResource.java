package com.slusarczykr.paxos.leader.resource;

import com.slusarczykr.paxos.leader.discovery.service.ServerDiscoveryService;
import com.slusarczykr.paxos.leader.discovery.state.PaxosServer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ServerResource {

    private final PaxosServer paxosServer;
    private final ServerDiscoveryService discoveryService;

    @GetMapping(value = "/status")
    public ResponseEntity<Void> status() {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping(value = "/leader")
    public ResponseEntity<String> isLeader() {
        String leader = String.valueOf(paxosServer.isLeader());
        return new ResponseEntity<>(leader, HttpStatus.OK);
    }

    @GetMapping(value = "/servers")
    public ResponseEntity<List<String>> getServers() {
        List<String> servers = new ArrayList<>(discoveryService.getAllServers().values());
        return new ResponseEntity<>(servers, HttpStatus.OK);
    }

    @GetMapping(value = "/available-servers")
    public ResponseEntity<List<String>> getAvailableServers() {
        List<String> servers = new ArrayList<>(discoveryService.getAvailableServers().values());
        return new ResponseEntity<>(servers, HttpStatus.OK);
    }
}
