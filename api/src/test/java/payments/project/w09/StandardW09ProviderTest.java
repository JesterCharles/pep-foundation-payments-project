package payments.project.w09;

import org.junit.jupiter.api.DisplayName;

import payments.project.support.Milestone;
import payments.project.support.RetryBoundChecks;

/** provider calls: one key per request, replayed answers, attempt limit (supplied; do not edit). */
@Milestone("standard-w09")
@DisplayName("Standard W09: provider calls reuse one key and stop at the attempt limit")
class StandardW09ProviderTest extends RetryBoundChecks {
}
