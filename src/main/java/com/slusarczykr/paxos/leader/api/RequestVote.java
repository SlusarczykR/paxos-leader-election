package com.slusarczykr.paxos.leader.api;

import com.slusarczykr.paxos.leader.api.AppendEntry;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

public class RequestVote extends AppendEntry implements Serializable {

    public RequestVote(long serverId, long term, long commitIndex) {
        super(serverId, term, commitIndex);
    }

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
        @EqualsAndHashCode(callSuper = true)
        public static class Accepted extends Response {

            public Accepted(long serverId) {
                super(true, serverId);
            }
        }

        @Data
        @EqualsAndHashCode(callSuper = true)
        public static class Rejected extends Response {

            public Rejected(long serverId) {
                super(false, serverId);
            }
        }
    }
}
