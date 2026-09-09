package org.jetbrains.bazel.clion

import com.intellij.testFramework.common.timeoutRunBlocking
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerId
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.assertions.assertNotNull
import org.jetbrains.bazel.assertions.assertThat
import org.jetbrains.bazel.assertions.assertVfsLoads
import org.jetbrains.bazel.assertions.findCompilerSetting
import org.jetbrains.bazel.assertions.findResolveConfiguration
import org.jetbrains.bazel.assertions.findTarget
import org.jetbrains.bazel.clion.workspace.getCcIdentifier
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@CcTestApplication
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArmToolchainTest {

  private val project by clionBazelProjectFixture("clion/arm_toolchain") {
    deriveTargetsFromDirectories(false)
    addTargets("//:main_u575")
  }

  @Test
  @Disabled("Correctly reports that we do cause indexing in bazel-bin for this project.")
  fun testVfsRoots() = project.assertVfsLoads(emptyList())

  /**
   * The platform_transition_binary transitions to a single platform, so main.c should have exactly one resolve
   * configuration.
   */
  @Test
  fun checkResolveConfiguration(): Unit = timeoutRunBlocking {
    val configuration = project.findResolveConfiguration("srcs/main.c")

    val identifier = configuration.getCcIdentifier().assertNotNull()

    // the target for //:main_u575 should not be present in the target map since it is not a cc_ target
    val targetInfo = project.findTarget("//:main")
    assertThat(targetInfo.key.configuration).isEqualTo(identifier.configurationId)
  }

  @Test
  fun testCompilerSettings(): Unit = timeoutRunBlocking {
    val compilerSettings = project.findCompilerSetting("srcs/main.c", CLanguageKind.C)

    assertThat(compilerSettings)
      .hasCompiler(OCCompilerId.GCC)
      .containsSwitches("-mcpu=cortex-m33", "-mthumb", "-DSTM32U575xx")
  }
}
