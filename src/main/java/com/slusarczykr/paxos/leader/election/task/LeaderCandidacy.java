package com.slusarczykr.paxos.leader.election.task;

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
    private LeaderElectionService leaderElectionService;

    public boolean start() {
        try {
            return leaderElectionService.startLeaderCandidacy();
        } catch (PaxosLeaderElectionException e) {
            log.error(e.getLocalizedMessage(), e);
        }
        return false;
    }
}
