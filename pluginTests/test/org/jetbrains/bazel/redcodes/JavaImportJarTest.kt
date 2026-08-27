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
class JavaImportJarTest  {

  private val fixture by bazelSyncCodeInsightFixture(
    "redcodes/java_import_jar",
  )

  @Test
  fun testHighlighting() = runBlocking(Dispatchers.Default) {
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("JarUsage.java")
    }
  }
}
