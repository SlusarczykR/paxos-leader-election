package com.slusarczykr.paxos.leader.election.task;

import com.slusarczykr.paxos.leader.election.service.LeaderElectionService;
import com.slusarczykr.paxos.leader.discovery.state.ServerDetails;
import com.slusarczykr.paxos.leader.exception.PaxosLeaderElectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.TimerTask;

@Configurable
public class StartLeaderElectionTask extends TimerTask {

    private static final Logger log = LoggerFactory.getLogger(StartLeaderElectionTask.class);

    @Autowired
    private ServerDetails serverDetails;

    @Autowired
    private LeaderElectionService leaderElectionService;

    public void run() {
        try {
            startLeaderCandidacy();
        } catch (PaxosLeaderElectionException e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }

    private void startLeaderCandidacy() throws PaxosLeaderElectionException {
        if (leaderElectionService.shouldCandidateForLeader()) {
            serverDetails.incrementTerm();
            candidateForLeader();
        }
    }

    private void candidateForLeader() throws PaxosLeaderElectionException {
        if (leaderElectionService.candidateForLeader()) {
            log.info("Server with id {} has been accepted by the majority and elected as the leader for the current turn!",
                    serverDetails.getIdValue());
        }
    }
}
