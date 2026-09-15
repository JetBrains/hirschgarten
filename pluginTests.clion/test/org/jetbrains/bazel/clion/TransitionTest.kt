package org.jetbrains.bazel.clion

import com.intellij.testFramework.common.timeoutRunBlocking
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.assertions.assertThat
import org.jetbrains.bazel.assertions.assertVfsLoads
import org.jetbrains.bazel.assertions.findCompilerSetting
import org.jetbrains.bazel.assertions.findCompilerSettings
import org.jetbrains.bazel.assertions.findResolveConfigurations
import org.jetbrains.bazel.assertions.findTargets
import org.jetbrains.bazel.clion.workspace.getCcIdentifier
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.jetbrains.bazel.resolveCompilerSwitches
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@CcTestApplication
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransitionTest {

  private val project by clionBazelProjectFixture("clion/transition") {
    deriveTargetsFromDirectories(false)
    addTargets("//main:simple_foo", "//main:simple_bar", "//main:shared_a", "//main:shared_b")
  }

  @Test
  fun testVfsRoots() = project.assertVfsLoads(emptyList())

  @Test
  fun testCompilerSettings(): Unit = timeoutRunBlocking {
    val compilerSettings = project.findCompilerSettings("main/simple.cc")
    assertThat(compilerSettings).hasSize(2)

    assertThat(compilerSettings.filter { it.resolveCompilerSwitches().contains("-DFOO") }).hasSize(1)
    assertThat(compilerSettings.filter { it.resolveCompilerSwitches().contains("-DBAR") }).hasSize(1)
  }

  @Test
  fun testWorkspaceSnapshot(): Unit = timeoutRunBlocking {
    val configurations = project.findResolveConfigurations("main/simple.cc").mapNotNull { it.getCcIdentifier()?.configurationId }
    assertThat(configurations).hasSize(2)

    val targets = project.findTargets("//main:simple")
    assertThat(targets).hasSize(2)

    assertThat(targets[0].key.configuration.shortChecksum).isNotNull()
    assertThat(configurations).contains(targets[0].key.configuration)

    assertThat(targets[1].key.configuration.shortChecksum).isNotNull()
    assertThat(configurations).contains(targets[1].key.configuration)

    assertThat(targets[0].key.configuration).isNotEqualTo(targets[1].key.configuration)
  }

  @Test
  fun testSharedConfiguration(): Unit = timeoutRunBlocking {
    val compilerSettings =  project.findCompilerSetting("main/shared.cc")
    assertThat(compilerSettings).containsSwitches("-DSHARED")
  }
}
