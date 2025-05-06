package no.sikt.nva.handle.exceptions;

import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;

public class CreateHandleException extends ApiGatewayException {

    public CreateHandleException(Exception exception, Integer statusCode) {
        super(exception, statusCode);
    }

    @JacocoGenerated
    @Override
    protected Integer statusCode() {
        return getStatusCode();
    }
}
