package org.jetbrains.bazel.test.framework

import com.goide.highlighting.GoAnnotator
import com.goide.highlighting.legacyErrorInspections.GoUnresolvedReferenceInspection

fun BazelSyncCodeInsightTestFixture.enableGoHighlighting() {
  GoAnnotator.enableChecks(testRootDisposable, ::GoUnresolvedReferenceInspection)
}
