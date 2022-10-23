package com.slusarczykr.paxos.leader.vote.service;

import com.slusarczykr.paxos.leader.api.RequestVote;

public interface RequestVoteService {

    RequestVote.Response vote(RequestVote requestVote);
}
