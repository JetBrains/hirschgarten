package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@BazelTestApplication
class KotlinCompilerPluginArgsTest {

  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  @Test
  @Disabled("KT-86287")
  fun testHighlighting() = runBlocking(Dispatchers.Default) {
    fixture.copyBazelTestProject("redcodes/kotlin_compiler_plugin_args")
    fixture.performBazelSync()
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("UserIsOpenTest.kt")
    }
  }
}
