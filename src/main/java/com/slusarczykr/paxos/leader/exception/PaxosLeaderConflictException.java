package com.slusarczykr.paxos.leader.exception;

public class PaxosLeaderConflictException extends RuntimeException {

    public PaxosLeaderConflictException(String message) {
        super(message);
    }
}
