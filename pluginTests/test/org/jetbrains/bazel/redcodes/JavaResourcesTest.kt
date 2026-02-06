package org.jetbrains.bazel.redcodes

import com.intellij.codeInspection.i18n.InvalidPropertyKeyInspection
import com.intellij.openapi.application.EDT
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import io.kotest.common.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.jetbrains.bazel.test.framework.setupJdk
import org.junit.jupiter.api.Test

@TestApplication
class JavaResourcesTest  {

  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  @Test
  fun testHighlighting() = runBlocking {
    fixture.setupJdk(IdeaTestUtil.getMockJdk21())
    fixture.enableInspections(InvalidPropertyKeyInspection())
    fixture.copyBazelTestProject("redcodes/java_resources")
    fixture.performBazelSync()
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("module/src/main/java/com/example/Module.java")
    }
  }
}
