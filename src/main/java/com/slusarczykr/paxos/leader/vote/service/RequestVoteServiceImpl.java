package com.slusarczykr.paxos.leader.vote.service;

import com.slusarczykr.paxos.leader.api.RequestVote;
import com.slusarczykr.paxos.leader.discovery.state.ServerDetails;
import com.slusarczykr.paxos.leader.vote.factory.RequestVoteFactory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static com.slusarczykr.paxos.leader.api.RequestVote.Response.Status.ACCEPTED;
import static com.slusarczykr.paxos.leader.api.RequestVote.Response.Status.REJECTED;

@Service
@RequiredArgsConstructor
public class RequestVoteServiceImpl implements RequestVoteService {

    private static final Logger log = LoggerFactory.getLogger(RequestVoteServiceImpl.class);

    private final ServerDetails serverDetails;

    private final RequestVoteFactory requestVoteFactory;

    @Override
    public RequestVote.Response vote(RequestVote requestVote) {
        log.info("Start voting procedure for leader election of candidate server with id {}...", requestVote.getServerId());
        long candidateTerm = requestVote.getTerm();
        boolean accepted = vote(candidateTerm);
        log.info(getServerCandidacyVotingStatusMessage(accepted, requestVote.getServerId()));

        return requestVoteFactory.create(requestVote.getServerId(), toResponseStatus(accepted));
    }

    private String getServerCandidacyVotingStatusMessage(boolean accepted, long candidateServerId) {
        String acceptanceMessage = accepted ? "accepted" : "rejected";
        return String.format("Candidacy of the server with id %d has been %s!", candidateServerId, acceptanceMessage);
    }

    private RequestVote.Response.Status toResponseStatus(boolean accepted) {
        return accepted ? ACCEPTED : REJECTED;
    }

    private boolean vote(long candidateTerm) {
        boolean accepted = voteForCandidate(candidateTerm);

        if (accepted) {
            setCurrentTerm(candidateTerm);
        }
        return accepted;
    }

    private void setCurrentTerm(long term) {
        serverDetails.updateTerm(term);
    }

    private boolean voteForCandidate(long candidateTerm) {
        long currentTerm = serverDetails.getTermValue();
        log.info("Current term: {}, candidate term: {}", currentTerm, candidateTerm);

        return candidateTerm > currentTerm;
    }
}
