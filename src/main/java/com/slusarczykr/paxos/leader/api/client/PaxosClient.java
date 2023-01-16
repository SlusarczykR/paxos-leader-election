package com.slusarczykr.paxos.leader.api.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slusarczykr.paxos.leader.api.AppendEntry;
import com.slusarczykr.paxos.leader.api.RequestVote;
import com.slusarczykr.paxos.leader.discovery.state.PaxosServer;
import com.slusarczykr.paxos.leader.exception.PaxosLeaderElectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
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

    private static final String FAKE_URI = "/fake";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PaxosServer paxosServer;

    public PaxosClient(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper, PaxosServer paxosServer) {
        this.restTemplate = restTemplateBuilder
                .errorHandler(new PaxosClientErrorHandler())
                .build();
        this.objectMapper = objectMapper;
        this.paxosServer = paxosServer;
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
            requestUrl = malformUrlIfLostConnectionEnabled(requestUrl);
            HttpEntity<String> request = toRequest(appendEntry);
            return Optional.ofNullable(restTemplate.postForObject(requestUrl, request, requestVoteResponse));
        } catch (IllegalStateException | JsonProcessingException e) {
            throw new PaxosLeaderElectionException("Error occurred on request processing!");
        } catch (Exception e) {
            log.error("Server listening on address {} is not reachable!", requestUrl, e);
        }
        return Optional.empty();
    }

    private String malformUrlIfLostConnectionEnabled(String requestUrl) {
        if (paxosServer.isLostConnectionEnabled()) {
            log.debug("Malforming request url: '{}'", requestUrl);
            return requestUrl + FAKE_URI;
        }
        return requestUrl;
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
