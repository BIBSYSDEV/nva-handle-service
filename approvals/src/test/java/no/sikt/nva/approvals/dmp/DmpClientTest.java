package no.sikt.nva.approvals.dmp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DmpClientTest {

    private static final String BASE_URL = "https://api.example.com/ctis";
    private static final String CLINICAL_TRIAL_IDENTIFIER = "2022-500027-76-00";
    private static final String CLINICAL_TRIAL_RESPONSE = """
        {
            "id": "https://api.example.com/ctis/clinical-trial/2022-500027-76-00",
            "identifier": "2022-500027-76-00",
            "handle": "https://hdl.handle.net/11250.1/39083745",
            "publicTitle": "Test Clinical Trial",
            "sponsors": [
                {
                    "type": "Sponsor",
                    "name": "Test Hospital"
                }
            ],
            "trialSites": [
                {
                    "type": "TrialSite",
                    "departmentName": "Test Department",
                    "investigator": {
                        "type": "Investigator",
                        "firstName": "John",
                        "lastName": "Doe",
                        "nvaPersonId": "https://api.nva.unit.no/cristin/person/12345"
                    }
                }
            ],
            "events": [
                {
                    "type": "TrialStart",
                    "region": "Norway",
                    "date": "2022-10-05"
                }
            ]
        }
        """;

    private HttpClient httpClient;
    private OAuth2TokenService tokenService;
    private DmpClient dmpClient;

    @BeforeEach
    void setUp() throws DmpClientException {
        httpClient = mock(HttpClient.class);
        tokenService = mock(OAuth2TokenService.class);
        when(tokenService.getAccessToken()).thenReturn("test-token");
    }

    @Test
    void shouldFetchClinicalTrialById() throws Exception {
        var response = createMockResponse(200, CLINICAL_TRIAL_RESPONSE);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        dmpClient = new DmpClient(tokenService, BASE_URL, httpClient);

        var result = dmpClient.getClinicalTrial(CLINICAL_TRIAL_IDENTIFIER);

        assertTrue(result.isPresent());
        var clinicalTrial = result.get();
        assertThat(clinicalTrial.identifier(), is(CLINICAL_TRIAL_IDENTIFIER));
        assertThat(clinicalTrial.publicTitle(), is("Test Clinical Trial"));
        assertThat(clinicalTrial.sponsors().size(), is(1));
        assertThat(clinicalTrial.trialSites().size(), is(1));
    }

    @Test
    void shouldReturnEmptyWhenClinicalTrialNotFound() throws Exception {
        var response = createMockResponse(404, "Not found");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        dmpClient = new DmpClient(tokenService, BASE_URL, httpClient);

        var result = dmpClient.getClinicalTrial(CLINICAL_TRIAL_IDENTIFIER);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldThrowExceptionOnServerError() throws Exception {
        var response = createMockResponse(500, "Internal server error");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        dmpClient = new DmpClient(tokenService, BASE_URL, httpClient);

        var exception = assertThrows(DmpClientException.class,
            () -> dmpClient.getClinicalTrial(CLINICAL_TRIAL_IDENTIFIER));

        assertThat(exception.getMessage(), notNullValue());
    }

    @Test
    void shouldThrowExceptionOnNetworkError() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenThrow(new IOException("Network error"));
        dmpClient = new DmpClient(tokenService, BASE_URL, httpClient);

        var exception = assertThrows(DmpClientException.class,
            () -> dmpClient.getClinicalTrial(CLINICAL_TRIAL_IDENTIFIER));

        assertThat(exception.getCause(), notNullValue());
    }

    @Test
    void shouldThrowExceptionWhenTokenServiceFails() throws Exception {
        when(tokenService.getAccessToken()).thenThrow(new DmpClientException("Token error"));
        dmpClient = new DmpClient(tokenService, BASE_URL, httpClient);

        assertThrows(DmpClientException.class, () -> dmpClient.getClinicalTrial(CLINICAL_TRIAL_IDENTIFIER));
    }

    @Test
    void shouldThrowExceptionWhenIdentifierIsNull() {
        dmpClient = new DmpClient(tokenService, BASE_URL, httpClient);

        assertThrows(NullPointerException.class, () -> dmpClient.getClinicalTrial(null));
    }

    @Test
    void shouldThrowExceptionWhenTokenServiceIsNull() {
        assertThrows(NullPointerException.class, () -> new DmpClient(null, BASE_URL, httpClient));
    }

    @Test
    void shouldThrowExceptionWhenBaseUrlIsNull() {
        assertThrows(NullPointerException.class, () -> new DmpClient(tokenService, null, httpClient));
    }

    @Test
    void shouldThrowExceptionWhenHttpClientIsNull() {
        assertThrows(NullPointerException.class, () -> new DmpClient(tokenService, BASE_URL, null));
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> createMockResponse(int statusCode, String body) {
        var response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(body);
        return response;
    }
}
