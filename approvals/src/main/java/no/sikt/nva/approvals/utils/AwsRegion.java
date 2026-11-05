package no.sikt.nva.approvals.utils;

import nva.commons.core.Environment;

public final class AwsRegion {

  private static final String AWS_REGION = "AWS_REGION";
  private static final String DEFAULT_REGION = "eu-west-1";

  private AwsRegion() {}

  public static String defaultRegionName(Environment environment) {
    return environment.readEnvOpt(AWS_REGION).orElse(DEFAULT_REGION);
  }
}
