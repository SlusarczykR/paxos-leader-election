package com.slusarczykr.paxos.leader.api;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

@Data
public class AppendEntry implements Serializable {

    private final long serverId;
    private final long term;
    private final long commitIndex;

    @Data
    @RequiredArgsConstructor
    public static class Response implements Serializable {

        private final long serverId;
    }
}
