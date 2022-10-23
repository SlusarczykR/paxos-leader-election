package com.slusarczykr.paxos.leader.election.handler;

import java.util.function.Function;

public interface LeaderElectionHandler<R> extends Function<String, R> {

    R apply(String url);
}
