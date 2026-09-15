package org.jetbrains.bazel.clion

import com.intellij.openapi.util.SystemInfoRt
import com.intellij.testFramework.common.timeoutRunBlocking
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerId
import org.jetbrains.bazel.assertions.assertThat
import org.jetbrains.bazel.assertions.assertVfsLoads
import org.jetbrains.bazel.assertions.findCompilerSetting
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@CcTestApplication
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CcLocalToolchainTest {

  private val project by clionBazelProjectFixture("clion/simple")

  @Test
  fun testVfsRoots() = project.assertVfsLoads(emptyList())

  @Test
  fun testCompilerSettings(): Unit = timeoutRunBlocking {
    val compilerSettingsC = project.findCompilerSetting("main/main.cc", language = CLanguageKind.C)
    val compilerSettingsCPP = project.findCompilerSetting("main/main.cc", language = CLanguageKind.CPP)
    val expectedCompiler = if (SystemInfoRt.isWindows) OCCompilerId.MSVC else OCCompilerId.GCC

    if (!SystemInfoRt.isWindows) {
      assertThat(compilerSettingsCPP).hasCompilerKindWrapper()
      assertThat(compilerSettingsC).hasCompilerKindWrapper()
    }

    assertThat(compilerSettingsCPP)
      .hasCompiler(expectedCompiler)
      .containsHeaders("iostream", "stdio.h")
      .containsSwitches("-Wall", "-DCXXOPTS")
      .doesNotContainSwitches("-DCONLYOPTS")
      .hasDefine("SIMPLE_DEFINE", "42")
      .hasDefine("SPACE_DEFINE", "1 2 3")

    assertThat(compilerSettingsC)
      .hasCompiler(expectedCompiler)
      .containsHeaders("stdio.h")
      .containsSwitches("-Wall", "-DCONLYOPTS")
      .doesNotContainSwitches("-DCXXOPTS")
      .hasDefine("SIMPLE_DEFINE", "42")
      .hasDefine("SPACE_DEFINE", "1 2 3")
  }
}
