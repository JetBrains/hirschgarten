package org.jetbrains.bazel.test.framework

import com.goide.highlighting.GoAnnotator
import com.goide.highlighting.legacyErrorInspections.GoInvalidPackageImportInspection
import com.goide.highlighting.legacyErrorInspections.GoUnresolvedImportInspection
import com.goide.highlighting.legacyErrorInspections.GoUnresolvedReferenceInspection
import org.jetbrains.bazel.golang.inspections.BazelGoStrictDependenciesInspection

fun BazelSyncCodeInsightTestFixture.enableGoHighlighting() {
  GoAnnotator.enableChecks(
    testRootDisposable,
    ::GoUnresolvedImportInspection,
    ::GoUnresolvedReferenceInspection,
    ::GoInvalidPackageImportInspection,
  )
  enableInspections(BazelGoStrictDependenciesInspection::class.java)
}
