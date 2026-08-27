package org.jetbrains.bazel.redcodes

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
class GoInternalImportTest {

  private val fixture by bazelSyncCodeInsightFixture(
    "redcodes/go_internal_import",
    configure = { it.enableGoHighlighting() },
  )

  @Test
  @DisabledOnOs(OS.WINDOWS)
  fun testHighlighting() = runBlocking(Dispatchers.Default) {
    fixture.checkHighlighting("foo/main.go")
  }
}
