package com.slusarczykr.paxos.leader.api.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slusarczykr.paxos.leader.api.AppendEntry;
import com.slusarczykr.paxos.leader.api.RequestVote;
import com.slusarczykr.paxos.leader.exception.PaxosLeaderElectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static com.slusarczykr.paxos.leader.api.client.PaxosEndpoints.HEARTBEAT_PATH;
import static com.slusarczykr.paxos.leader.api.client.PaxosEndpoints.VOTE_PATH;

@Component
public class PaxosClient {

    private static final Logger log = LoggerFactory.getLogger(PaxosClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public PaxosClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    public Optional<RequestVote.Response> requestCandidates(String serverLocation, RequestVote requestVote) throws PaxosLeaderElectionException {
        String requestUrl = buildServerLeaderCandidacyVoteUrl(serverLocation);
        return sendRequest(requestUrl, requestVote, RequestVote.Response.class);
    }

    public Optional<AppendEntry.Response> sendHeartbeats(String serverLocation, AppendEntry appendEntry) throws PaxosLeaderElectionException {
        String requestUrl = buildServerLeaderHeartbeatUrl(serverLocation);
        return sendRequest(requestUrl, appendEntry, AppendEntry.Response.class);
    }

    private <T> Optional<T> sendRequest(String requestUrl, AppendEntry appendEntry, Class<T> requestVoteResponse)
            throws PaxosLeaderElectionException {
        try {
            return Optional.ofNullable(restTemplate.postForObject(requestUrl, toRequest(appendEntry), requestVoteResponse));
        } catch (JsonProcessingException e) {
            throw new PaxosLeaderElectionException("Error while proposing the leader candidacy!");
        } catch (Exception e) {
            log.error("Server listening on address {} is not reachable!", requestUrl, e);
        }
        return Optional.empty();
    }

    private HttpEntity<String> toRequest(AppendEntry appendEntry) throws JsonProcessingException {
        return new HttpEntity<>(toJSON(appendEntry), prepareHeaders());
    }

    private String toJSON(AppendEntry requestVote) throws JsonProcessingException {
        return objectMapper.writeValueAsString(requestVote);
    }

    private HttpHeaders prepareHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return headers;
    }

    private String buildServerLeaderHeartbeatUrl(String serverLocation) {
        return serverLocation + HEARTBEAT_PATH;
    }

    private String buildServerLeaderCandidacyVoteUrl(String serverLocation) {
        return serverLocation + VOTE_PATH;
    }
}
