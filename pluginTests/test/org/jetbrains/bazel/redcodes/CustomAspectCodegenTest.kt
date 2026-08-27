package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.intellij.testFramework.common.timeoutRunBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

@BazelTestApplication
class CustomAspectCodegenTest {

  private val fixture by bazelSyncCodeInsightFixture(
    "redcodes/custom_aspect_codegen",
    buildProject = true,
    bazelVersion = "9.1.0",
  )

  @Test
  fun testCustomAspectGeneratedClassResolves() = timeoutRunBlocking(timeout = 5.minutes) {
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("App.java")
    }
  }
}
