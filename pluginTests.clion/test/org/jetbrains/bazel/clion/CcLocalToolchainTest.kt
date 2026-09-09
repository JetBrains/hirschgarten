package org.jetbrains.bazel.clion

import com.intellij.testFramework.common.timeoutRunBlocking
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerId
import org.jetbrains.bazel.assertions.assertThat
import org.jetbrains.bazel.assertions.assertVfsLoads
import org.jetbrains.bazel.assertions.findCompilerSetting
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@CcTestApplication
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CcLocalToolchainTest {

  private val project by clionBazelProjectFixture("clion/simple")

  @Test
  @Disabled("Correctly reports that we do cause indexing in bazel-bin for this project.")
  fun testVfsRoots() = project.assertVfsLoads(emptyList())

  @Test
  fun testCompilerSettings(): Unit = timeoutRunBlocking {
    val compilerSettingsC = project.findCompilerSetting("main/main.cc", language = CLanguageKind.C)
    val compilerSettingsCPP = project.findCompilerSetting("main/main.cc", language = CLanguageKind.CPP)

    assertThat(compilerSettingsCPP)
      .hasCompiler(OCCompilerId.GCC)
      .containsHeaders("iostream", "stdio.h")
      .containsSwitches("-Wall", "-DCXXOPTS")
      .doesNotContainSwitches("-DCONLYOPTS")
      .containsDefines("SIMPLE_DEFINE=42", "SPACE_DEFINE=1 2 3")

    assertThat(compilerSettingsC)
      .hasCompiler(OCCompilerId.GCC)
      .containsHeaders("stdio.h")
      .containsSwitches("-Wall", "-DCONLYOPTS")
      .doesNotContainSwitches("-DCXXOPTS")
      .containsDefines("SIMPLE_DEFINE=42", "SPACE_DEFINE=1 2 3")
  }
}
