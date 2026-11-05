package no.sikt.nva.approvals.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import nva.commons.core.Environment;
import org.junit.jupiter.api.Test;

class AwsRegionTest {

  private static final String AWS_REGION = "AWS_REGION";
  private static final String CONFIGURED_REGION = "eu-north-1";
  private static final String DEFAULT_REGION = "eu-west-1";

  @Test
  void shouldReturnConfiguredRegion() {
    var environment = mock(Environment.class);
    when(environment.readEnvOpt(AWS_REGION)).thenReturn(Optional.of(CONFIGURED_REGION));

    assertEquals(CONFIGURED_REGION, AwsRegion.defaultRegionName(environment));
  }

  @Test
  void shouldReturnDefaultRegionWhenNoneIsConfigured() {
    var environment = mock(Environment.class);
    when(environment.readEnvOpt(AWS_REGION)).thenReturn(Optional.empty());

    assertEquals(DEFAULT_REGION, AwsRegion.defaultRegionName(environment));
  }
}
