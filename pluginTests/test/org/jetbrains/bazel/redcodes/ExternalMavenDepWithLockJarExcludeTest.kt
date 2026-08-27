package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS

@BazelTestApplication
class ExternalMavenDepWithLockJarExcludeTest {

  private val fixture by bazelSyncCodeInsightFixture(
    "redcodes/external_maven_dep_with_lock_jar_exclude",
  )

  @Test
  @DisabledOnOs(OS.WINDOWS) // coursier
  fun testHighlighting() = runBlocking(Dispatchers.Default) {
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("Usage.java")
    }
  }
}
