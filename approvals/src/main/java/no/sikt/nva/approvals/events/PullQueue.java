package no.sikt.nva.approvals.events;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface PullQueue {

  void enqueue(PullRequest pullRequest);
}
