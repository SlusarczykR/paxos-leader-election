package com.slusarczykr.paxos.leader.api;

import lombok.Data;

import java.io.Serializable;

@Data
public class AppendEntry implements Serializable {

    private final long serverId;
    private final long term;
    private final long commitIndex;
}
