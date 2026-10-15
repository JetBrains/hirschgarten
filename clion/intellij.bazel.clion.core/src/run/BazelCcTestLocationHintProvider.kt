package org.jetbrains.bazel.clion.run

import com.intellij.util.io.URLUtil
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.clion.sync.includesCc
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.testing.BazelTestDetails
import org.jetbrains.bazel.util.BazelTestLocationHintProvider

@ApiStatus.Internal
const val BAZEL_CC_PROTOCOL: String = "bazel:test:cc"

@ApiStatus.Internal
const val TEST_NAME_PARTS_SPLITTER: String = "::"

/**
 * We don't know beforehand which testing framework is used, so we use a generic protocol instead
 */
internal class BazelCcTestLocationHintProvider : BazelTestLocationHintProvider {
  override fun isApplicable(targetKind: TargetKind): Boolean = targetKind.includesCc()

  override fun getLocationHint(testDetails: BazelTestDetails): String =
    BAZEL_CC_PROTOCOL + URLUtil.SCHEME_SEPARATOR + testDetails.parentSuiteNames.firstOrNull()
      .orEmpty() + TEST_NAME_PARTS_SPLITTER + testDetails.displayName
}
