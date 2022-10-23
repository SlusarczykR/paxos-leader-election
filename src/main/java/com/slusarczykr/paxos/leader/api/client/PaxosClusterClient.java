package com.slusarczykr.paxos.leader.api.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slusarczykr.paxos.leader.api.RequestVote;
import com.slusarczykr.paxos.leader.exception.PaxosLeaderElectionException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PaxosClusterClient {

    private static final Logger log = LoggerFactory.getLogger(PaxosClusterClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public <T extends RequestVote.Response> Optional<T> requestCandidates(String serverLocation, RequestVote requestVote,
                                                                          Class<T> requestVoteResponse) throws PaxosLeaderElectionException {
        try {
            String requestUrl = buildServerLeaderCandidacyVoteUrl(serverLocation);
            HttpEntity<String> request = toRequest(requestVote);
            return Optional.ofNullable(restTemplate.postForObject(requestUrl, request, requestVoteResponse));
        } catch (JsonProcessingException e) {
            throw new PaxosLeaderElectionException("Error while proposing the leader candidacy!");
        } catch (Exception e) {
            log.warn("Server listening on address {} is not reachable!", serverLocation);
        }
        return Optional.empty();
    }

    private HttpEntity<String> toRequest(RequestVote requestVote) throws JsonProcessingException {
        return new HttpEntity<>(toJSON(requestVote), prepareHeaders());
    }

    private String toJSON(RequestVote requestVote) throws JsonProcessingException {
        return objectMapper.writeValueAsString(requestVote);
    }

    private HttpHeaders prepareHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return headers;
    }

    private String buildServerLeaderCandidacyVoteUrl(String serverLocation) {
        return serverLocation + "/leaderElection/vote";
    }
}
