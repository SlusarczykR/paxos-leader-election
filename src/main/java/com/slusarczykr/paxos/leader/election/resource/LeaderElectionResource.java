package com.slusarczykr.paxos.leader.election.resource;

import com.slusarczykr.paxos.leader.api.AppendEntry;
import com.slusarczykr.paxos.leader.api.RequestVote;
import com.slusarczykr.paxos.leader.discovery.state.PaxosServer;
import com.slusarczykr.paxos.leader.election.service.LeaderElectionService;
import com.slusarczykr.paxos.leader.exception.PaxosLeaderElectionException;
import com.slusarczykr.paxos.leader.starter.LeaderElectionStarter;
import com.slusarczykr.paxos.leader.vote.service.RequestVoteService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(path = "leaderElection", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class LeaderElectionResource {

    private static final Logger log = LoggerFactory.getLogger(LeaderElectionResource.class);

    private final LeaderElectionService leaderElectionService;
    private final LeaderElectionStarter leaderElectionStarter;
    private final RequestVoteService requestVoteService;
    private final PaxosServer paxosServer;

    @PostMapping("/candidate")
    public ResponseEntity<Void> candidateForLeader() {
        try {
            return startLeaderCandidacy();
        } catch (PaxosLeaderElectionException e) {
            log.error("Unable start server candidacy for a leader!", e);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Void> startLeaderCandidacy() throws PaxosLeaderElectionException {
        boolean leader = leaderElectionService.startLeaderCandidacy();

        if (leader) {
            return new ResponseEntity<>(HttpStatus.CREATED);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/vote")
    public ResponseEntity<RequestVote.Response> voteForLeaderCandidate(@RequestBody RequestVote requestVote) {
        log.info("Received vote from server with id: {}", requestVote.getServerId());
        stopHeartbeatsOrReset();
        RequestVote.Response requestVoteResponse = requestVoteService.vote(requestVote);

        return new ResponseEntity<>(requestVoteResponse, HttpStatus.OK);
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<AppendEntry.Response> sendHeartbeat(@RequestBody AppendEntry appendEntry) {
        log.info("Received heartbeat from leader with id: {}", appendEntry.getServerId());
        AppendEntry.Response appendEntryResponse = new AppendEntry.Response(paxosServer.getIdValue());

        if (stopHeartbeatsOrReset()) {
            log.error("Heartbeat message received while the current server is already the leader!");
            return new ResponseEntity<>(appendEntryResponse, HttpStatus.CONFLICT);
        }
        return new ResponseEntity<>(appendEntryResponse, HttpStatus.OK);
    }

    private boolean stopHeartbeatsOrReset() {
        boolean leader = paxosServer.isLeader();

        if (leader) {
            log.info("Stopping sending heartbeats...");
            leaderElectionStarter.stopHeartbeats();
        } else {
            leaderElectionStarter.reset();
        }
        return leader;
    }
}
