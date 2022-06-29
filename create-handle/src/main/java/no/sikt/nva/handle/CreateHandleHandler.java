package no.sikt.nva.handle;

import com.amazonaws.services.lambda.runtime.Context;
import no.sikt.nva.handle.exceptions.MalformedRequestException;
import no.sikt.nva.handle.model.CreateHandleRequest;
import no.sikt.nva.handle.model.CreateHandleResponse;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;

import java.net.HttpURLConnection;

import static java.util.Objects.isNull;

public class CreateHandleHandler extends ApiGatewayHandler<CreateHandleRequest, CreateHandleResponse> {

    public static final String NULL_URI_ERROR = "uri can not be null";
    private final HandleDatabase handleDatabase;

    @JacocoGenerated
    public CreateHandleHandler() {
        this(new HandleDatabase());
    }

    public CreateHandleHandler(HandleDatabase handleDatabase) {
        super(CreateHandleRequest.class);
        this.handleDatabase = handleDatabase;
    }

    @Override
    protected CreateHandleResponse processInput(CreateHandleRequest input, RequestInfo requestInfo, Context context)
            throws ApiGatewayException {
        validate(input);
        return new CreateHandleResponse(handleDatabase.createHandle(input.getUri()));
    }

    @Override
    protected Integer getSuccessStatusCode(CreateHandleRequest input, CreateHandleResponse output) {
        return HttpURLConnection.HTTP_CREATED;
    }

    private void validate(CreateHandleRequest input) throws MalformedRequestException {
        if (isNull(input) || isNull(input.getUri())) {
            throw new MalformedRequestException(NULL_URI_ERROR);
        }
    }
}
