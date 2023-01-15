package com.slusarczykr.paxos.leader.discovery.state;

import lombok.Data;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class ErrorStatus {

    private final Map<Type, Boolean> errorToStatus;

    public ErrorStatus() {
        this.errorToStatus = Arrays.stream(Type.values())
                .collect(Collectors.toMap(it -> it, it -> false));
    }

    public boolean isEnabled(Type type) {
        return errorToStatus.get(type);
    }

    public void enable(Type type) {
        updateErrorStatus(type, true);
    }

    public void disable(Type type) {
        updateErrorStatus(type, false);
    }

    private void updateErrorStatus(Type type, boolean status) {
        errorToStatus.put(type, status);
    }

    public enum Type {
        LOST_CONNECTION, INVALID_RESPONSE, INFINITE_REPLIES
    }
}
