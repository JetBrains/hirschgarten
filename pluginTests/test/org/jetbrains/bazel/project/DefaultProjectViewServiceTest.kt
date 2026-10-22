package org.jetbrains.bazel.project

import com.intellij.openapi.application.ReadAction
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@BazelTestApplication
internal class DefaultProjectViewServiceTest {
  private val testBazel by testBazelProjectFixture()

  @Test
  fun `project view file lookup should not deadlock when in read action`() { // BAZEL-3356
    assertTimeoutPreemptively(5.seconds.toJavaDuration()) {
      ReadAction.runBlocking<Throwable> {
        DefaultProjectViewService(testBazel.project).projectViewFile
      }
    }
  }
}
