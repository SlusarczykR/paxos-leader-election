package com.slusarczykr.paxos.leader.election.service;

import com.slusarczykr.paxos.leader.api.AppendEntry;
import com.slusarczykr.paxos.leader.api.RequestVote;
import com.slusarczykr.paxos.leader.api.client.PaxosClient;
import com.slusarczykr.paxos.leader.discovery.service.ServerDiscoveryService;
import com.slusarczykr.paxos.leader.discovery.state.PaxosServer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaderElectionServiceImpl implements LeaderElectionService {

    private static final Logger log = LoggerFactory.getLogger(LeaderElectionServiceImpl.class);

    private final PaxosClient paxosClient;

    private final PaxosServer paxosServer;
    private final ServerDiscoveryService discoveryService;

    @Override
    public RequestVote createElectionVote() {
        return new RequestVote(
                paxosServer.getIdValue(),
                paxosServer.getTermValue(),
                paxosServer.getCommitIndexValue()
        );
    }

    @Override
    public boolean startLeaderCandidacy() {
        paxosServer.incrementTerm();

        if (shouldCandidateForLeader()) {
            return candidateForLeader();
        }
        return false;
    }

    private boolean candidateForLeader() {
        log.info("Starting the candidacy of the server with id {} for the leader...", paxosServer.getIdValue());
        RequestVote requestVote = createElectionVote();
        List<RequestVote.Response> responseRequestVotes = sendRequestVoteToFollowers(requestVote);
        boolean accepted = checkAcceptanceMajority(responseRequestVotes);
        paxosServer.setLeader(accepted);

        if (accepted) {
            log.info("Server with id {} has been accepted by the majority and elected as the leader for the current turn!",
                    paxosServer.getIdValue());
        }
        return accepted;
    }

    private List<RequestVote.Response> sendRequestVoteToFollowers(RequestVote requestVote) {
        return getServerAddresses().stream()
                .map(serverLocation -> sendRequestVote(requestVote, serverLocation))
                .flatMap(Optional::stream)
                .toList();
    }

    @SneakyThrows
    private Optional<RequestVote.Response> sendRequestVote(RequestVote requestVote, String serverLocation) {
        return paxosClient.sendRequestVote(serverLocation, requestVote);
    }

    private <T extends RequestVote.Response> boolean checkAcceptanceMajority(List<T> responseRequestVotes) {
        Map<Boolean, List<T>> candidatesResponsesByAcceptance = getCandidatesResponsesByAcceptance(responseRequestVotes);
        boolean acceptedByMajority = isAcceptedByMajority(candidatesResponsesByAcceptance);
        log.info(getServerCandidacyVotingStatusMessage(acceptedByMajority));

        return acceptedByMajority;
    }

    private String getServerCandidacyVotingStatusMessage(boolean acceptedByMajority) {
        String acceptanceMessage = acceptedByMajority ? "accepted" : "rejected";
        return String.format("Candidacy of the server with id %d has been %s in the current turn!",
                paxosServer.getIdValue(), acceptanceMessage);
    }

    private <T extends RequestVote.Response> Map<Boolean, List<T>> getCandidatesResponsesByAcceptance(List<T> responseRequestVotes) {
        long currentTerm = paxosServer.getTermValue();

        return responseRequestVotes.stream()
                .map(it -> negateVoteAcceptanceIfCorrupted(it, currentTerm))
                .collect(Collectors.partitioningBy(RequestVote.Response::isAccepted));
    }

    private <T extends RequestVote.Response> T negateVoteAcceptanceIfCorrupted(T responseRequestVote, long currentTerm) {
        if (isVoteCorrupted(responseRequestVote, currentTerm)) {
            log.debug("Vote from server with id '{}' is corrupted. Acceptance value will be corrected", responseRequestVote.getServerId());
            responseRequestVote.setAccepted(!responseRequestVote.isAccepted());
        }
        return responseRequestVote;
    }

    private <T extends RequestVote.Response> boolean isVoteCorrupted(T responseRequestVote, long currentTerm) {
        return responseRequestVote.isAccepted() && responseRequestVote.getTerm() != currentTerm
                || !responseRequestVote.isAccepted() && currentTerm > responseRequestVote.getTerm();
    }

    private <T extends RequestVote.Response> boolean isAcceptedByMajority(Map<Boolean, List<T>> promisesByAcceptance) {
        return countAcceptedRequestVotes(promisesByAcceptance) > countRejectedRequestVotes(promisesByAcceptance, false);
    }

    private <T extends RequestVote.Response> int countRejectedRequestVotes(Map<Boolean, List<T>> promisesByAcceptance, boolean accepted) {
        return promisesByAcceptance.get(accepted).size();
    }

    private <T extends RequestVote.Response> int countAcceptedRequestVotes(Map<Boolean, List<T>> promisesByAcceptance) {
        int candidateRequestVote = 1;
        return countRejectedRequestVotes(promisesByAcceptance, true) + candidateRequestVote;
    }

    @Override
    public boolean shouldCandidateForLeader() {
        if (discoveryService.anyServerAvailable()) {
            boolean candidateForLeader = calculateCurrentTermModulo() == paxosServer.getIdValue();
            log.info(getShouldCandidateForLeaderMessage(candidateForLeader));

            return candidateForLeader;
        }
        log.debug("No servers available...");
        return true;
    }

    @Override
    public void sendHeartbeats(Consumer<Exception> errorHandler) {
        try {
            log.info("Sending heartbeats to followers...");
            AppendEntry appendEntry = createHeartbeat();
            getServerAddresses().stream()
                    .map(serverLocation -> sendHeartbeats(appendEntry, serverLocation))
                    .flatMap(Optional::stream)
                    .forEach(it -> log.info("Received heartbeat reply from follower with id: {}", it.getServerId()));
        } catch (Exception e) {
            log.error("Error occurred while sending heartbeats to followers!");
            errorHandler.accept(e);
        }
    }

    private Set<String> getServerAddresses() {
        return new HashSet<>(discoveryService.getAvailableServers().values());
    }

    @Override
    public AppendEntry createHeartbeat() {
        return new AppendEntry(
                paxosServer.getIdValue(),
                paxosServer.getTermValue(),
                paxosServer.getCommitIndexValue()
        );
    }

    @SneakyThrows
    private Optional<AppendEntry.Response> sendHeartbeats(AppendEntry appendEntry, String serverLocation) {
        return paxosClient.sendHeartbeats(serverLocation, appendEntry);
    }

    private String getShouldCandidateForLeaderMessage(boolean candidateForLeader) {
        String ableness = candidateForLeader ? "can" : "cannot";
        return String.format("Server %s candidate for a leader in the current turn...", ableness);
    }

    private long calculateCurrentTermModulo() {
        long serverTerm = paxosServer.getTermValue();
        int numberOfAvailableServers = discoveryService.getNumberOfAvailableServers();
        log.info("Number of available servers: {}, current server term: {}", numberOfAvailableServers, serverTerm);

        return serverTerm % numberOfAvailableServers;
    }
}
