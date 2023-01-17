package com.slusarczykr.paxos.leader.election.service;

import com.slusarczykr.paxos.leader.api.AppendEntry;
import com.slusarczykr.paxos.leader.api.RequestVote;
import com.slusarczykr.paxos.leader.exception.PaxosLeaderElectionException;

import java.util.function.Consumer;

public interface LeaderElectionService {

    RequestVote createElectionVote();

    boolean startLeaderCandidacy() throws PaxosLeaderElectionException;

    boolean shouldCandidateForLeader();

    AppendEntry createHeartbeat();

    void sendHeartbeats(Consumer<Exception> errorHandler);
}
