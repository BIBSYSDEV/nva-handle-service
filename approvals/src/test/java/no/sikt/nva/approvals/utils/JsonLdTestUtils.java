package no.sikt.nva.approvals.utils;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import no.sikt.nva.approvals.rest.FetchContextHandler;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;

public final class JsonLdTestUtils {

  public static final String APPROVAL_ONTOLOGY_NAMESPACE = "https://localhost/approval/ontology#";
  public static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
  private static final String XSD_STRING = "http://www.w3.org/2001/XMLSchema#string";

  private JsonLdTestUtils() {}

  public static JsonObject expandWithServedContext(String document)
      throws IOException, JsonLdError {
    return JsonLd.expand(JsonDocument.of(new StringReader(document)))
        .context(fetchServedContext())
        .get()
        .getJsonObject(0);
  }

  public static List<Triple> toTriplesWithApprovalContext(String document)
      throws IOException, JsonLdError {
    var triples = new ArrayList<Triple>();
    JsonLd.toRdf(JsonDocument.of(new StringReader(document)))
        .context(fetchServedContext())
        .provide(
            (subject, predicate, object, datatype, language, direction, graph) -> {
              triples.add(new Triple(subject, predicate, object, datatype));
              return null;
            });
    return List.copyOf(triples);
  }

  public static String objectOf(List<Triple> triples, String subject, String predicate) {
    return triples.stream()
        .filter(triple -> triple.subject().equals(subject) && triple.predicate().equals(predicate))
        .map(Triple::object)
        .findFirst()
        .orElseThrow();
  }

  public record Triple(String subject, String predicate, String object, String datatype) {

    public static Triple of(String subject, String predicate, String resource) {
      return new Triple(subject, predicate, resource, null);
    }

    public static Triple literal(String subject, String predicate, String value) {
      return new Triple(subject, predicate, value, XSD_STRING);
    }

    public static Triple typed(String subject, String predicate, String value, String datatype) {
      return new Triple(subject, predicate, value, datatype);
    }
  }

  private static JsonObject fetchServedContext() throws IOException {
    var outputStream = new ByteArrayOutputStream();
    var request = new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper).build();
    new FetchContextHandler().handleRequest(request, outputStream, new FakeContext());
    var context = GatewayResponse.fromOutputStream(outputStream, String.class).getBody();
    return Json.createReader(new StringReader(context)).readObject();
  }
}
