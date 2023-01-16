package com.slusarczykr.paxos.leader.api.client;

import com.slusarczykr.paxos.leader.exception.PaxosLeaderConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;

@Component
public class PaxosClientErrorHandler implements ResponseErrorHandler {

    @Override
    public boolean hasError(ClientHttpResponse httpResponse)
            throws IOException {
        HttpStatus.Series statusCodeSeries = httpResponse.getStatusCode().series();

        return isClientOrServerError(statusCodeSeries);
    }

    @Override
    public void handleError(ClientHttpResponse httpResponse) throws IOException {
        HttpStatus.Series statusCodeSeries = httpResponse.getStatusCode().series();

        if (isClientOrServerError(statusCodeSeries)) {
            String errorMessage = String.format("Error with status %s occurred during request", statusCodeSeries.name());
            throw new PaxosLeaderConflictException(errorMessage);
        }
    }

    private boolean isClientOrServerError(HttpStatus.Series statusCodeSeries) {
        return statusCodeSeries == CLIENT_ERROR || statusCodeSeries == SERVER_ERROR;
    }
}
