package com.slusarczykr.paxos.leader.election.service;

import com.slusarczykr.paxos.leader.model.RequestVote;
import com.slusarczykr.paxos.leader.exception.PaxosLeaderElectionException;

public interface LeaderElectionService {

    RequestVote createElectionVote();

    boolean candidateForLeader() throws PaxosLeaderElectionException;

    boolean shouldCandidateForLeader() throws PaxosLeaderElectionException;
}
