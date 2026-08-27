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
class AnnotationProcessorTest {

  private val fixture by bazelSyncCodeInsightFixture("redcodes/annotation_processor", buildProject = true)

  @Test
  @DisabledOnOs(OS.WINDOWS) // hangs
  fun testHighlighting() = runBlocking(Dispatchers.Default) {
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("JavaLib.java")
      fixture.checkHighlighting("KtLib.kt")
      fixture.checkHighlighting("KtTestLib.kt")
    }
  }
}
