package no.sikt.nva.handle;

import static java.util.Objects.isNull;
import static no.sikt.nva.handle.HandleDatabase.ERROR_UPDATING_HANDLE_FOR_URI;
import static no.sikt.nva.handle.utils.DatabaseConnectionSupplier.getConnectionSupplier;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.sql.Connection;
import java.util.function.Supplier;
import no.sikt.nva.handle.exceptions.MalformedRequestException;
import no.sikt.nva.handle.exceptions.UpdateHandleException;
import no.sikt.nva.handle.model.HandleRequest;
import no.sikt.nva.handle.model.HandleResponse;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JacocoGenerated
public class UpdateHandleHandler extends ApiGatewayHandler<HandleRequest, HandleResponse> {

    public static final String NULL_URI_ERROR = "uri can not be null";
    private static final Logger logger = LoggerFactory.getLogger(UpdateHandleHandler.class);
    private final HandleDatabase handleDatabase;
    private final Supplier<Connection> connectionSupplier;

    @JacocoGenerated
    public UpdateHandleHandler() {
        this(new Environment(), getConnectionSupplier());
    }

    public UpdateHandleHandler(Environment environment, Supplier<Connection> connectionSupplier) {
        super(HandleRequest.class, environment);
        this.handleDatabase = new HandleDatabase(environment);
        this.connectionSupplier = connectionSupplier;
    }

    @Override
    protected void validateRequest(HandleRequest createHandleRequest, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        validate(createHandleRequest);
    }

    @Override
    protected HandleResponse processInput(HandleRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var prefix = requestInfo.getPathParameter("prefix");
        var suffix = requestInfo.getPathParameter("suffix");
        try (var connection = connectionSupplier.get()) {
            return new HandleResponse(handleDatabase.updateHandle(prefix, suffix, input.uri(), connection));
        } catch (Exception e) {
            var message = getNestedExceptionMessage(String.format(ERROR_UPDATING_HANDLE_FOR_URI, prefix, suffix), e);
            logger.error(message, e);
            throw new UpdateHandleException(message);
        }
    }

    private static String getNestedExceptionMessage(String message, Exception e) {
        return isNull(e.getMessage()) ? message : e.getMessage();
    }

    @Override
    protected Integer getSuccessStatusCode(HandleRequest input, HandleResponse output) {
        return HttpURLConnection.HTTP_OK;
    }

    private void validate(HandleRequest input) throws MalformedRequestException {
        if (isNull(input) || isNull(input.uri())) {
            throw new MalformedRequestException(NULL_URI_ERROR);
        }
    }
}
