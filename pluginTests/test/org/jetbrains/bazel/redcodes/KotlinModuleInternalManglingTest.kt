package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test

@BazelTestApplication
class KotlinModuleInternalManglingTest {

  private val fixture by bazelSyncCodeInsightFixture(
    "redcodes/kotlin_module_internal_mangling",
  )

  @Test
  fun testHighlighting() = runBlocking(Dispatchers.Default) {
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("B.java")
    }
  }
}
