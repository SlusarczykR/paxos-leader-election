package com.slusarczykr.paxos.leader.election.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slusarczykr.paxos.leader.model.RequestVote;
import com.slusarczykr.paxos.leader.discovery.service.ServerDiscoveryService;
import com.slusarczykr.paxos.leader.discovery.state.ServerDetails;
import com.slusarczykr.paxos.leader.exception.PaxosLeaderElectionException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaderElectionServiceImpl implements LeaderElectionService {

    private static final Logger log = LoggerFactory.getLogger(LeaderElectionServiceImpl.class);

    private final RestTemplate webClient;
    private final ObjectMapper objectMapper;

    private final ServerDetails serverDetails;
    private final ServerDiscoveryService discoveryService;

    @Override
    public RequestVote createElectionVote() {
        return new RequestVote(
                serverDetails.getIdValue(),
                serverDetails.getTermValue(),
                serverDetails.getCommitIndexValue()
        );
    }

    @Override
    public boolean candidateForLeader() throws PaxosLeaderElectionException {
        try {
            log.info("Starting the candidacy of the server with id {} for the leader...", serverDetails.getIdValue());
            HttpEntity<String> requestVote = prepareElectionRequestVote();
            List<RequestVote.Response> responseRequestVotes = sendRequestVoteToCandidates(requestVote);

            return checkAcceptanceMajority(responseRequestVotes);
        } catch (JsonProcessingException e) {
            throw new PaxosLeaderElectionException("Error while proposing the leader!");
        }
    }

    private HttpEntity<String> prepareElectionRequestVote() throws JsonProcessingException {
        RequestVote electionVote = createElectionVote();
        return new HttpEntity<>(toJSON(electionVote), prepareHeaders());
    }

    private List<RequestVote.Response> sendRequestVoteToCandidates(HttpEntity<String> requestVote) {
        return discoveryService.getServers().values().stream()
                .map(serverLocation -> requestCandidates(serverLocation, requestVote, RequestVote.Response.class))
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    private String buildServerLeaderCandidacyVoteUrl(String serverLocation) {
        return serverLocation + "/leaderElection/vote";
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
                serverDetails.getIdValue(), acceptanceMessage);
    }

    private <T extends RequestVote.Response> Map<Boolean, List<T>> getCandidatesResponsesByAcceptance(List<T> responseRequestVotes) {
        return responseRequestVotes.stream()
                .collect(Collectors.partitioningBy(RequestVote.Response::isAccepted));
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

    private String toJSON(RequestVote requestVote) throws JsonProcessingException {
        return objectMapper.writeValueAsString(requestVote);
    }

    private HttpHeaders prepareHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return headers;
    }

    private <T extends RequestVote.Response> Optional<T> requestCandidates(String serverLocation, HttpEntity<String> request,
                                                                           Class<T> requestVoteResponse) {
        try {
            String requestUrl = buildServerLeaderCandidacyVoteUrl(serverLocation);
            return Optional.ofNullable(webClient.postForObject(requestUrl, request, requestVoteResponse));
        } catch (Exception e) {
            log.warn("Server listening on address {} is not reachable!", serverLocation);
        }
        return Optional.empty();
    }

    @Override
    public boolean shouldCandidateForLeader() throws PaxosLeaderElectionException {
        boolean candidateForLeader = calculateCurrentTermModulo() == serverDetails.getIdValue();
        log.info(getShouldCandidateForLeaderMessage(candidateForLeader));

        return candidateForLeader;
    }

    private String getShouldCandidateForLeaderMessage(boolean candidateForLeader) {
        String ableness = candidateForLeader ? "can" : "cannot";
        return String.format("Server %s candidate for a leader in the current turn...", ableness);
    }

    private long calculateCurrentTermModulo() {
        long serverTerm = serverDetails.getTermValue();
        int numberOfAvailableServers = discoveryService.getNumberOfAvailableServers();
        log.info("Number of available servers: {}, current server term: {}", numberOfAvailableServers, serverTerm);

        return serverTerm % numberOfAvailableServers;
    }
}
