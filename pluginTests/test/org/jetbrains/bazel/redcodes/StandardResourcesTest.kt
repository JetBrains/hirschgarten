package org.jetbrains.bazel.redcodes

import com.intellij.codeInspection.i18n.InvalidPropertyKeyInspection
import com.intellij.openapi.application.EDT
import com.intellij.testFramework.IdeaTestUtil
import io.kotest.common.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelSyncCodeInsightFixtureTestCase
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.jetbrains.bazel.test.framework.setupJdk

class StandardResourcesTest : BazelSyncCodeInsightFixtureTestCase() {

  fun testHighlighting() = runBlocking {
    myFixture.setupJdk(IdeaTestUtil.getMockJdk21())
    myFixture.enableInspections(InvalidPropertyKeyInspection())
    myFixture.copyDirectoryToProject("redcodes/standard_resources", "")
    myFixture.performBazelSync()
    withContext(Dispatchers.EDT) {
      myFixture.checkHighlighting("A.java")
    }
  }
}
