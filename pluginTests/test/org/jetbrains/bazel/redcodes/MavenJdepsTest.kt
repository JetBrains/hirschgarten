package org.jetbrains.bazel.redcodes

import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS

@BazelTestApplication
@DisabledOnOs(
  OS.WINDOWS,
  disabledReason = "Test passes on a Windows laptop locally but fails to build the test project on CI for some reason",
)
class MavenJdepsTest {
  private val fixture by bazelSyncCodeInsightFixture(
    "redcodes/maven_jdeps",
    buildProject = true,
    projectView = ".bazelproject",
  )

  @Test
  fun testHighlighting() = runBlocking {
    fixture.checkHighlighting("Main.java")
  }
}
