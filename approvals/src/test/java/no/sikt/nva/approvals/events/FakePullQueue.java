package no.sikt.nva.approvals.events;

import java.util.ArrayList;
import java.util.List;

public class FakePullQueue implements PullQueue {

  private final List<PullRequest> pullRequests = new ArrayList<>();

  @Override
  public void enqueue(PullRequest pullRequest) {
    pullRequests.add(pullRequest);
  }

  public List<PullRequest> getPullRequests() {
    return List.copyOf(pullRequests);
  }
}
