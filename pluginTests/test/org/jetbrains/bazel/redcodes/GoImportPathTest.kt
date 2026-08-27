package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.jetbrains.bazel.test.framework.enableGoHighlighting
import org.junit.jupiter.api.Test

@BazelTestApplication
class GoImportPathTest {

  private val fixture by bazelSyncCodeInsightFixture(
    "redcodes/go_import_path",
    configure = { it.enableGoHighlighting() },
  )

  @Test
  fun testHighlighting() = runBlocking(Dispatchers.Default) {
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("main/main.go")
      fixture.checkHighlighting("split/y/f2.go")
    }
  }
}
