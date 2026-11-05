package org.jetbrains.bazel.golang.run

import com.goide.execution.testing.GoTestLocator
import com.goide.utils.GoSharedConstants
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.golang.targetKinds.includesGo
import org.jetbrains.bazel.run.test.BazelTestLocatorProvider
import org.jetbrains.bazel.testing.BazelTestDetails
import org.jetbrains.bazel.util.BazelTestLocationHintProvider

internal class BazelGoTestLocationHintProvider : BazelTestLocationHintProvider {
  override fun isApplicable(targetKind: TargetKind): Boolean = targetKind.includesGo()

  override fun getLocationHint(testDetails: BazelTestDetails): String? {
    val suiteName = if (testDetails.isSuite) {
      testDetails.displayName
    }
    else {
      testDetails.parentSuiteNames.firstOrNull() ?: return null
    }
    val importPathDotSeparator = suiteName.lastIndexOf('.')
    if (importPathDotSeparator == -1) return null
    val importPath = suiteName.substring(0..<importPathDotSeparator)
    val outerTestName = suiteName.substring(importPathDotSeparator + 1)
    val subtestName = if (!testDetails.isSuite) testDetails.displayName else null
    val testName = if (outerTestName == subtestName || subtestName == null) {
      outerTestName
    }
    else {
      "$outerTestName/$subtestName"
    }
    return GoSharedConstants.TEST_PROTOCOL + "://" + importPath + "#" + testName
  }
}

internal class BazelGoTestLocatorProvider : BazelTestLocatorProvider {
  private val locator = GoTestLocator(null)

  override fun getTestLocator(): SMTestLocator = locator
}
