package org.jetbrains.bazel.flow

import com.intellij.ide.trustedProjects.TrustedProjects
import com.intellij.testFramework.junit5.SystemProperty
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.intellij.util.ThreeState
import org.jetbrains.bazel.commons.constants.Constants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.io.path.createFile

@TestApplication
@SystemProperty("idea.trust.headless.disabled", "false")
internal class BazelTrustedProjectsLocatorTest {
  private val projectRoot by tempPathFixture()

  @Test
  fun `trust state is remembered by Bazel project root`() {
    val moduleFile = projectRoot.resolve(Constants.MODULE_BAZEL_FILE_NAME).createFile()
    val projectViewFile = projectRoot.resolve("sample.bazelproject").createFile()

    TrustedProjects.setProjectTrusted(moduleFile, true)
    assertEquals(ThreeState.YES, TrustedProjects.getProjectTrustedState(projectViewFile))

    TrustedProjects.setProjectTrusted(projectViewFile, false)
    assertEquals(ThreeState.NO, TrustedProjects.getProjectTrustedState(moduleFile))
  }
}
