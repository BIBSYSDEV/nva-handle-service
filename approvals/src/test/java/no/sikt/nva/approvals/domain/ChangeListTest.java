package no.sikt.nva.approvals.domain;

import static no.sikt.nva.approvals.utils.TestUtils.randomSourceSnapshot;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ChangeListTest {

  @Test
  void shouldContinueAfterLastChangeWhenMoreChangesExist() {
    var last = randomSourceSnapshot();
    var changeList = new ChangeList(List.of(randomSourceSnapshot(), last), true);

    assertThat(changeList.next(), equalTo(Optional.of(last.identifier())));
  }

  @Test
  void shouldHaveNoNextWhenNoMoreChangesExist() {
    var changeList = new ChangeList(List.of(randomSourceSnapshot()), false);

    assertThat(changeList.next(), equalTo(Optional.empty()));
  }
}
