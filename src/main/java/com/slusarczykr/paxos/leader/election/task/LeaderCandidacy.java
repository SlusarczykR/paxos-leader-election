package com.slusarczykr.paxos.leader.election.task;

import com.slusarczykr.paxos.leader.discovery.state.ServerDetails;
import com.slusarczykr.paxos.leader.election.service.LeaderElectionService;
import com.slusarczykr.paxos.leader.exception.PaxosLeaderElectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

@Configurable
public class LeaderCandidacy {

    private static final Logger log = LoggerFactory.getLogger(LeaderCandidacy.class);

    @Autowired
    private ServerDetails serverDetails;

    @Autowired
    private LeaderElectionService leaderElectionService;

    public Boolean start() {
        try {
            return startLeaderCandidacy();
        } catch (PaxosLeaderElectionException e) {
            log.error(e.getLocalizedMessage(), e);
        }
        return false;
    }

    private boolean startLeaderCandidacy() throws PaxosLeaderElectionException {
        serverDetails.incrementTerm();

        if (leaderElectionService.shouldCandidateForLeader()) {
            return candidateForLeader();
        }
        return false;
    }

    private boolean candidateForLeader() throws PaxosLeaderElectionException {
        boolean leader = leaderElectionService.candidateForLeader();
        serverDetails.setLeader(leader);

        if (leader) {
            log.info("Server with id {} has been accepted by the majority and elected as the leader for the current turn!",
                    serverDetails.getIdValue());
        }
        return leader;
    }
}
