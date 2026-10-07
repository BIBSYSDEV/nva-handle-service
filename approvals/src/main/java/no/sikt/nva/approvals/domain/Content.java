package no.sikt.nva.approvals.domain;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nva.commons.core.attempt.Try.attempt;

import java.security.MessageDigest;
import java.util.HexFormat;

public record Content(String type, String body, String hash) {

  private static final String HASH_ALGORITHM = "SHA-256";

  public static Content create(String type, String body) {
    return new Content(type, body, hash(body));
  }

  private static String hash(String body) {
    var digest = attempt(() -> MessageDigest.getInstance(HASH_ALGORITHM)).orElseThrow();
    return HexFormat.of().formatHex(digest.digest(body.getBytes(UTF_8)));
  }
}
