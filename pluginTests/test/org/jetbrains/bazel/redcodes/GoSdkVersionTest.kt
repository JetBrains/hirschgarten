package org.jetbrains.bazel.redcodes

import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.jetbrains.bazel.test.framework.enableGoHighlighting
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS

@BazelTestApplication
class GoSdkVersionTest {
  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  @Test
  @DisabledOnOs(OS.WINDOWS)
  fun testHighlighting() = runBlocking(Dispatchers.Default) {
    fixture.enableGoHighlighting()
    fixture.copyBazelTestProject("redcodes/go_sdk_version")
    fixture.performBazelSync()
    fixture.checkHighlighting("foo.go")
  }
}
