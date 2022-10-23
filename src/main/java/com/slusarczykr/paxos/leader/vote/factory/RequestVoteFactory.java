package com.slusarczykr.paxos.leader.vote.factory;

import com.slusarczykr.paxos.leader.api.RequestVote;
import org.springframework.stereotype.Component;

@Component
public class RequestVoteFactory {

    public RequestVote.Response create(long serverId, RequestVote.Response.Status status) {
        if (RequestVote.Response.Status.ACCEPTED.equals(status)) {
            return new RequestVote.Response.Accepted(serverId);
        } else if (RequestVote.Response.Status.REJECTED.equals(status)) {
            return new RequestVote.Response.Rejected(serverId);
        }
        return null;
    }
}
