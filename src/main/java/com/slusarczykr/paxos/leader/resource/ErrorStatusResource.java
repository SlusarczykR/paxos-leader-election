package com.slusarczykr.paxos.leader.resource;

import com.slusarczykr.paxos.leader.discovery.state.ErrorStatus;
import com.slusarczykr.paxos.leader.discovery.state.PaxosServer;
import com.slusarczykr.paxos.leader.starter.LeaderElectionStarter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.function.Consumer;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(path = "error-status", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ErrorStatusResource {

    private final PaxosServer paxosServer;
    private final LeaderElectionStarter leaderElectionStarter;
    private final Converter<String, ErrorStatus.Type> errorStatusTypeConverter;

    @PostMapping(value = "/enable/{errorType}")
    public ResponseEntity<Void> enable(@PathVariable("errorType") String errorType) {
        HttpStatus httpStatus = withErrorStatusTypeConversion(errorType, this::enableError);
        return new ResponseEntity<>(httpStatus);
    }

    @PostMapping(value = "/disable/{errorType}")
    public ResponseEntity<Void> disable(@PathVariable("errorType") String errorType) {
        HttpStatus httpStatus = withErrorStatusTypeConversion(errorType, this::disableError);
        return new ResponseEntity<>(httpStatus);
    }

    private HttpStatus withErrorStatusTypeConversion(String errorType, Consumer<ErrorStatus.Type> action) {
        return convert(errorType)
                .map(it -> {
                    action.accept(it);
                    return HttpStatus.OK;
                })
                .orElse(HttpStatus.BAD_REQUEST);
    }

    private void enableError(ErrorStatus.Type errorType) {
        paxosServer.enableError(errorType);

        if (shouldUpdateHeartbeats(errorType)) {
            leaderElectionStarter.scheduleHeartbeats();
        }
    }

    private void disableError(ErrorStatus.Type errorType) {
        paxosServer.disableError(errorType);

        if (shouldUpdateHeartbeats(errorType)) {
            leaderElectionStarter.stopHeartbeats();
        }
    }

    private Optional<ErrorStatus.Type> convert(String errorType) {
        return Optional.ofNullable(errorStatusTypeConverter.convert(errorType));
    }

    private boolean shouldUpdateHeartbeats(ErrorStatus.Type errorType) {
        return errorType == ErrorStatus.Type.INFINITE_REPLIES && !paxosServer.isLeader();
    }
}
