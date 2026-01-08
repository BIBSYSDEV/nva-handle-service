package no.sikt.nva.approvals.dmp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OAuth2TokenServiceTest {

    private static final String ACCESS_TOKEN = "test-access-token";
    private static final String TOKEN_RESPONSE = """
        {
            "access_token": "%s",
            "expires_in": 3600,
            "token_type": "Bearer"
        }
        """.formatted(ACCESS_TOKEN);
    private static final String ERROR_RESPONSE = "Invalid client credentials";

    private HttpClient httpClient;
    private DmpClientSecrets secrets;
    private OAuth2TokenService tokenService;

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        secrets = new DmpClientSecrets(
            "client-id",
            "client-secret",
            "https://auth.example.com/oauth/token",
            "api://default",
            "https://api.example.com"
        );
    }

    @Test
    void shouldFetchAccessToken() throws Exception {
        var response = createMockResponse(200, TOKEN_RESPONSE);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        tokenService = new OAuth2TokenService(secrets, httpClient);

        var token = tokenService.getAccessToken();

        assertThat(token, is(ACCESS_TOKEN));
    }

    @Test
    void shouldCacheTokenAndReuseIt() throws Exception {
        var response = createMockResponse(200, TOKEN_RESPONSE);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        tokenService = new OAuth2TokenService(secrets, httpClient);

        tokenService.getAccessToken();
        tokenService.getAccessToken();

        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void shouldThrowExceptionOnAuthenticationFailure() throws Exception {
        var response = createMockResponse(401, ERROR_RESPONSE);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        tokenService = new OAuth2TokenService(secrets, httpClient);

        var exception = assertThrows(DmpClientException.class, () -> tokenService.getAccessToken());

        assertThat(exception.getMessage(), notNullValue());
    }

    @Test
    void shouldThrowExceptionOnNetworkError() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenThrow(new IOException("Network error"));
        tokenService = new OAuth2TokenService(secrets, httpClient);

        var exception = assertThrows(DmpClientException.class, () -> tokenService.getAccessToken());

        assertThat(exception.getMessage(), notNullValue());
        assertThat(exception.getCause(), notNullValue());
    }

    @Test
    void shouldThrowExceptionWhenSecretsAreNull() {
        assertThrows(NullPointerException.class, () -> new OAuth2TokenService(null, httpClient));
    }

    @Test
    void shouldThrowExceptionWhenHttpClientIsNull() {
        assertThrows(NullPointerException.class, () -> new OAuth2TokenService(secrets, null));
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> createMockResponse(int statusCode, String body) {
        var response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(body);
        return response;
    }
}
