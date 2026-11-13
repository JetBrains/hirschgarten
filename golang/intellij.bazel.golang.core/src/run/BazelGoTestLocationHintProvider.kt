package org.jetbrains.bazel.golang.run

import com.goide.execution.testing.GoTestLocator
import com.goide.utils.GoSharedConstants
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.golang.targetKinds.includesGo
import org.jetbrains.bazel.run.test.BazelTestLocatorProvider
import org.jetbrains.bazel.run.testResults.BazelTestLocationHintProvider
import org.jetbrains.bazel.testing.BazelTestDetails

internal class BazelGoTestLocationHintProvider : BazelTestLocationHintProvider {
  override fun isApplicable(targetKind: TargetKind): Boolean = targetKind.includesGo()

  override fun getLocationHint(testDetails: BazelTestDetails): String? {
    val suiteName = if (testDetails.isSuite) {
      testDetails.displayName
    }
    else {
      testDetails.parentSuiteNames.firstOrNull() ?: return null
    }
    val importPath = suiteName.substringBeforeLast('.', "")
    val outerTestName = suiteName.substringAfterLast('.')
    val testName =
      when {
        testDetails.isSuite || testDetails.displayName == outerTestName -> outerTestName
        testDetails.displayName.startsWith("$outerTestName/") -> testDetails.displayName
        else -> "$outerTestName/${testDetails.displayName}"
      }
    return GoSharedConstants.TEST_PROTOCOL + "://" + importPath + "#" + testName
  }
}

internal class BazelGoTestLocatorProvider : BazelTestLocatorProvider {
  private val locator = GoTestLocator(null)

  override fun getTestLocator(): SMTestLocator = locator
}
