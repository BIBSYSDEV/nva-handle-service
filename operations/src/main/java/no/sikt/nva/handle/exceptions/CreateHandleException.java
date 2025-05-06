package no.sikt.nva.handle.exceptions;

import nva.commons.apigateway.exceptions.ApiGatewayException;

public class CreateHandleException extends ApiGatewayException {

    public CreateHandleException(Exception exception, Integer statusCode) {
        super(exception, statusCode);
    }

    @Override
    protected Integer statusCode() {
        return getStatusCode();
    }
}
