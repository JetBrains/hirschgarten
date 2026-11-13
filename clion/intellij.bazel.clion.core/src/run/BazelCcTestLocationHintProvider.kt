package org.jetbrains.bazel.clion.run

import com.intellij.util.io.URLUtil
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.clion.sync.includesCc
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.run.testResults.BazelTestLocationHintProvider
import org.jetbrains.bazel.testing.BazelTestDetails

@ApiStatus.Internal
const val BAZEL_CC_PROTOCOL: String = "bazel:test:cc"

private const val TEST_NAME_PARTS_SPLITTER: String = "::"

/**
 * We don't know beforehand which testing framework is used, so we use a generic protocol instead.
 * The path holds the suite, the test and the optional parameter. A test name can contain any character, so each part is URL-encoded.
 */
internal class BazelCcTestLocationHintProvider : BazelTestLocationHintProvider {
  override fun isApplicable(targetKind: TargetKind): Boolean = targetKind.includesCc()

  override fun getLocationHint(testDetails: BazelTestDetails): String {
    val parts = listOfNotNull(
      testDetails.parentSuiteNames.firstOrNull().orEmpty(),
      testDetails.displayName,
      testDetails.valueParam ?: testDetails.typeParam,
    )
    return BAZEL_CC_PROTOCOL + URLUtil.SCHEME_SEPARATOR + parts.joinToString(TEST_NAME_PARTS_SPLITTER) { URLUtil.encodeURIComponent(it) }
  }
}

/** The suite, the test and the optional parameter of a [BAZEL_CC_PROTOCOL] location path. */
@ApiStatus.Internal
fun splitCcTestLocationPath(path: String): List<String> = path.split(TEST_NAME_PARTS_SPLITTER).map { URLUtil.decode(it) }
