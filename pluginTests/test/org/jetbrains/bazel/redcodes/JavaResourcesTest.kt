package org.jetbrains.bazel.redcodes

import com.intellij.codeInspection.i18n.InvalidPropertyKeyInspection
import com.intellij.openapi.application.EDT
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test

@BazelTestApplication
class JavaResourcesTest  {

  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  @Test
  fun testHighlighting() = runBlocking(Dispatchers.Default) {
    fixture.enableInspections(InvalidPropertyKeyInspection())
    fixture.copyBazelTestProject("redcodes/java_resources")
    fixture.performBazelSync()
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("module/src/main/java/com/example/Module.java")
    }
  }
}
