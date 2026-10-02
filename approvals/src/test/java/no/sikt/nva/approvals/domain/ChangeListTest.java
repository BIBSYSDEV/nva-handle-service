package no.sikt.nva.approvals.domain;

import static no.sikt.nva.approvals.utils.TestUtils.randomSourceChange;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ChangeListTest {

  @Test
  void shouldContinueAfterLastChangeWhenMoreChangesExist() {
    var last = SourceSnapshot.create(randomSourceChange());
    var changeList =
        new ChangeList(List.of(SourceSnapshot.create(randomSourceChange()), last), true);

    assertThat(changeList.next(), equalTo(Optional.of(last.identifier())));
  }

  @Test
  void shouldHaveNoNextWhenNoMoreChangesExist() {
    var changeList = new ChangeList(List.of(SourceSnapshot.create(randomSourceChange())), false);

    assertThat(changeList.next(), equalTo(Optional.empty()));
  }
}
