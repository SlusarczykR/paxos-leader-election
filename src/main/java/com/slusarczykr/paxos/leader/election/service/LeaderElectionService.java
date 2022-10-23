package com.slusarczykr.paxos.leader.election.service;

import com.slusarczykr.paxos.leader.exception.PaxosLeaderElectionException;
import com.slusarczykr.paxos.leader.api.RequestVote;

public interface LeaderElectionService {

    RequestVote createElectionVote();

    boolean candidateForLeader() throws PaxosLeaderElectionException;

    boolean shouldCandidateForLeader();

    void sendHeartbeats();
}
