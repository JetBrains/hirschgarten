package org.jetbrains.bazel.clion

import com.intellij.testFramework.common.timeoutRunBlocking
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.cidr.lang.workspace.OCWorkspace
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.clion.workspace.CC_CLIENT_KEY
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.ProjectViewBuilder
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.jetbrains.bazel.sync.ProjectSyncScope
import org.jetbrains.bazel.sync.status.syncAndCheckOutcome
import org.jetbrains.bazel.test.framework.BazelVersions
import org.jetbrains.bazel.test.framework.writeProjectView
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.time.Duration.Companion.minutes

@CcTestApplication
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CcResyncTest {

  private val project by clionBazelProjectFixture("clion/simple", buildProject = true, bazelVersion = BazelVersions.BAZEL_9)

  @Test
  fun testResyncWithoutCcTargetsRemovesTheResolveConfigurations(): Unit = timeoutRunBlocking(timeout = 10.minutes) {
    assertThat(configurations()).isNotEmpty()

    val projectView = ProjectViewBuilder()
      .addDirectories(".")
      .addTargets("//toolchain:empty")
      .deriveTargetsFromDirectories(false)
      .build()
    writeProjectView(project, projectView)
    project.syncAndCheckOutcome(ProjectSyncScope.Full(build = false, phased = false))

    assertThat(configurations()).isEmpty()
  }

  private fun configurations(): List<OCResolveConfiguration> = OCWorkspace.getInstance(project).getConfigurations(CC_CLIENT_KEY)
}
