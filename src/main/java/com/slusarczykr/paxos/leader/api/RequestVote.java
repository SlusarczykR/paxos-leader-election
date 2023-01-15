package com.slusarczykr.paxos.leader.api;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

public class RequestVote extends AppendEntry implements Serializable {

    public RequestVote(long serverId, long term, long commitIndex) {
        super(serverId, term, commitIndex);
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Response.Accepted.class, name = "accepted"),
            @JsonSubTypes.Type(value = Response.Rejected.class, name = "rejected")
    })
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public abstract static class Response implements Serializable {

        public enum Status {
            ACCEPTED, REJECTED
        }

        private boolean accepted;
        private long serverId;

        @Data
        @NoArgsConstructor
        @EqualsAndHashCode(callSuper = true)
        public static class Accepted extends Response {

            public Accepted(long serverId) {
                super(true, serverId);
            }
        }

        @Data
        @NoArgsConstructor
        @EqualsAndHashCode(callSuper = true)
        public static class Rejected extends Response {

            public Rejected(long serverId) {
                super(false, serverId);
            }
        }
    }
}