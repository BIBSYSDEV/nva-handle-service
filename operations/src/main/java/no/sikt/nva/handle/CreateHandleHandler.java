package no.sikt.nva.handle;

import com.amazonaws.services.lambda.runtime.Context;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;
import no.sikt.nva.handle.exceptions.CreateHandleException;
import no.sikt.nva.handle.exceptions.MalformedRequestException;
import no.sikt.nva.handle.model.HandleRequest;
import no.sikt.nva.handle.model.HandleResponse;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import java.net.HttpURLConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.isNull;
import static no.sikt.nva.handle.HandleDatabase.ERROR_CREATING_HANDLE_FOR_URI;
import static no.sikt.nva.handle.utils.DatabaseConnectionSupplier.getConnectionSupplier;

public class CreateHandleHandler extends ApiGatewayHandler<HandleRequest, HandleResponse> {

    private static final String NULL_URI_ERROR = "uri can not be null";
    private static final Logger logger = LoggerFactory.getLogger(CreateHandleHandler.class);
    private final HandleDatabase handleDatabase;
    private final Supplier<Connection> connectionSupplier;

    @JacocoGenerated
    public CreateHandleHandler() {
        this(new Environment(), getConnectionSupplier());
    }

    public CreateHandleHandler(Environment environment, Supplier<Connection> connectionSupplier) {
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
        try (var connection = connectionSupplier.get()) {
            return createHandle(input, connection);
        } catch (Exception e) {
            var message = getNestedExceptionMessage(String.format(ERROR_CREATING_HANDLE_FOR_URI, input.uri()), e);
            logger.error(message, e);
            throw new CreateHandleException(message);
        }
    }

    private HandleResponse createHandle(HandleRequest input, Connection connection)
        throws SQLException {
        try {
            logger.info("Creating handle for uri: {}", input.uri());
            var handle = handleDatabase.createHandle(input.uri(), connection);
            connection.commit();
            return new HandleResponse(handle);
        } catch (Exception e) {
            connection.rollback();
            throw e;
        }
    }

    private static String getNestedExceptionMessage(String message, Exception e) {
        return isNull(e.getMessage()) ? message : e.getMessage();
    }

    @Override
    protected Integer getSuccessStatusCode(HandleRequest input, HandleResponse output) {
        return HttpURLConnection.HTTP_CREATED;
    }

    private void validate(HandleRequest input) throws MalformedRequestException {
        if (isNull(input) || isNull(input.uri())) {
            throw new MalformedRequestException(NULL_URI_ERROR);
        }
    }
}
