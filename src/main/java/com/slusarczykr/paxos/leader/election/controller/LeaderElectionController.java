package com.slusarczykr.paxos.leader.election.controller;

import com.slusarczykr.paxos.leader.election.service.LeaderElectionService;
import com.slusarczykr.paxos.leader.model.RequestVote;
import com.slusarczykr.paxos.leader.vote.service.RequestVoteService;
import com.slusarczykr.paxos.leader.exception.PaxosLeaderElectionException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("leaderElection")
@RequiredArgsConstructor
public class LeaderElectionController {

    private static final Logger log = LoggerFactory.getLogger(LeaderElectionController.class);

    private final LeaderElectionService leaderElectionService;
    private final RequestVoteService requestVoteService;

    @PostMapping(value = "/candidate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> candidateForLeader() {
        try {
            return startLeaderCandidacy();
        } catch (PaxosLeaderElectionException e) {
            log.error("Unable start server candidacy for a leader!", e);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Void> startLeaderCandidacy() throws PaxosLeaderElectionException {
        if (leaderElectionService.shouldCandidateForLeader()) {
            leaderElectionService.candidateForLeader();
            return new ResponseEntity<>(HttpStatus.CREATED);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping(value = "/vote", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RequestVote.Response> voteForLeaderCandidate(@RequestBody RequestVote requestVote) {
        RequestVote.Response requestVoteResponse = requestVoteService.vote(requestVote);
        return new ResponseEntity<>(requestVoteResponse, HttpStatus.OK);
    }
}
