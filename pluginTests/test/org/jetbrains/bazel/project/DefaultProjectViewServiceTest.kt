package org.jetbrains.bazel.project

import com.intellij.openapi.application.ReadAction
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

internal class DefaultProjectViewServiceTest : MockProjectBaseTest() {
  private val tempDirFixture = tempPathFixture()
  private val tempDir by tempDirFixture

  @Test
  fun `project view file lookup should not deadlock when in read action`() { // BAZEL-3356
    initializeBazelProject(project, tempDir)
    assertTimeoutPreemptively(5.seconds.toJavaDuration()) {
      ReadAction.runBlocking<Throwable> {
        DefaultProjectViewService(project).projectViewFile
      }
    }
  }
}
