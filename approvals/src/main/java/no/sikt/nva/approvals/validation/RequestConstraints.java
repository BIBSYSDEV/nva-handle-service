package no.sikt.nva.approvals.validation;

public final class RequestConstraints {

  public static final int MIN_IDENTIFIERS = 1;
  public static final int MAX_IDENTIFIERS = 20;
  public static final int MAX_IDENTIFIER_NAME_LENGTH = 100;
  public static final int MAX_IDENTIFIER_VALUE_BYTES = 900;
  public static final int MAX_URI_LENGTH = 1024;
  public static final String IDENTIFIER_NAME_PATTERN = "\\s*[A-Za-z0-9_-]+\\s*";
  public static final String EVENT_ID_PATTERN = "[A-Za-z0-9_-]{1,128}";
  public static final String SUPPORTED_SPEC_VERSION = "1.0";
  public static final String SUPPORTED_EVENT_TYPE = "no.sikt.nva.approval.source.changed";
  public static final String SPEC_VERSION_PATTERN = "\\Q" + SUPPORTED_SPEC_VERSION + "\\E";
  public static final String EVENT_TYPE_PATTERN = "\\Q" + SUPPORTED_EVENT_TYPE + "\\E";

  public static final String MANDATORY_MESSAGE = "Is mandatory";
  public static final String IDENTIFIERS_SIZE_MESSAGE =
      "Between {min} and {max} identifiers are required";
  public static final String TOO_LONG_MESSAGE = "Must be at most {max} characters long";
  public static final String TOO_MANY_BYTES_MESSAGE = "Must be at most {max} bytes long in UTF-8";
  public static final String IDENTIFIER_NAME_PATTERN_MESSAGE =
      "May only contain letters, digits, hyphen and underscore";
  public static final String DUPLICATE_IDENTIFIER_MESSAGE =
      "Identifier with the same name and value is provided more than once";
  public static final String UNSUPPORTED_SPEC_VERSION_MESSAGE =
      "Unsupported specversion, only " + SUPPORTED_SPEC_VERSION + " is supported";
  public static final String UNSUPPORTED_EVENT_TYPE_MESSAGE =
      "Unsupported event type, only " + SUPPORTED_EVENT_TYPE + " is supported";
  public static final String INVALID_EVENT_ID_MESSAGE = "Must match " + EVENT_ID_PATTERN;
  public static final String NOT_A_HANDLE_MESSAGE = "Must be the handle of an approval";

  private RequestConstraints() {}
}
