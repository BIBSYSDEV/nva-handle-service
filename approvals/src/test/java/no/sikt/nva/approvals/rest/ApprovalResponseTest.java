package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;

class ApprovalResponseTest {

    private static final URI BASE_URI = URI.create("https://api.unittest.nva.unit.no/approval");

    @Test
    void shouldConvertApprovalToResponse() {
        var handle = randomHandle();
        var approval = randomApproval(handle);

        var response = ApprovalResponse.fromApproval(approval, BASE_URI);

        assertEquals(approval.identifier(), response.identifier());
        assertEquals(approval.namedIdentifiers(), response.identifiers());
        assertEquals(approval.source(), response.source());
        assertEquals(handle.value().toString(), response.handle());
    }

    @Test
    void shouldGenerateIdFromRequestUriHostAndIdentifier() {
        var handle = randomHandle();
        var approval = randomApproval(handle);

        var response = ApprovalResponse.fromApproval(approval, BASE_URI);

        var expectedId = URI.create("https://" + BASE_URI.getHost() + "/approval/" + approval.identifier());
        assertEquals(expectedId, response.id());
    }

    @Test
    void shouldSerializeHandleAsString() throws JsonProcessingException {
        var handle = randomHandle();
        var approval = randomApproval(handle);
        var response = ApprovalResponse.fromApproval(approval, BASE_URI);

        var json = JsonUtils.dtoObjectMapper.writeValueAsString(response);
        var jsonNode = JsonUtils.dtoObjectMapper.readTree(json);

        assertTrue(jsonNode.has("handle"));
        assertInstanceOf(String.class, jsonNode.get("handle").textValue());
        assertEquals(handle.value().toString(), jsonNode.get("handle").textValue());
    }

    @Test
    void shouldRoundTripThroughJson() throws JsonProcessingException {
        var handle = randomHandle();
        var approval = randomApproval(handle);
        var response = ApprovalResponse.fromApproval(approval, BASE_URI);

        var json = JsonUtils.dtoObjectMapper.writeValueAsString(response);
        var deserialized = JsonUtils.dtoObjectMapper.readValue(json, ApprovalResponse.class);

        assertEquals(response, deserialized);
    }
}
