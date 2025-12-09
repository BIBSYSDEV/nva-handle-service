package no.sikt.nva.approvals;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import com.amazonaws.services.lambda.runtime.Context;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;

public class CreateApprovalHandler extends ApiGatewayHandler<Void, Void> {

    public CreateApprovalHandler() {
        super(Void.class, new Environment());
    }

    @Override
    protected void validateRequest(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {

    }

    @Override
    protected Void processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Void output) {
        return HTTP_ACCEPTED;
    }
}
