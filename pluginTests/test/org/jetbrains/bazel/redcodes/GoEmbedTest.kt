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
class GoEmbedTest {

  private val fixture by bazelSyncCodeInsightFixture(
    "redcodes/go_embed",
    configure = { it.enableGoHighlighting() },
  )

  @Test
  fun testHighlighting() = runBlocking(Dispatchers.Default) {
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("lib/lib.go")
      fixture.checkHighlighting("lib_embed/lib_embed.go")
      fixture.checkHighlighting("main/main.go")
    }
  }
}
