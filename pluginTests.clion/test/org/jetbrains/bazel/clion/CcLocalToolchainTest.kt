package org.jetbrains.bazel.clion

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
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS

@CcTestApplication
@EnabledOnOs(OS.LINUX)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CcLocalToolchainTest {

  private val project by clionBazelProjectFixture("clion/simple")

  @Test
  fun testVfsRoots() = project.assertVfsLoads(emptyList())

  @Test
  fun testCompilerSettings(): Unit = timeoutRunBlocking {
    val compilerSettingsC = project.findCompilerSetting("main/util.c", language = CLanguageKind.C)
    val compilerSettingsCPP = project.findCompilerSetting("main/main.cc", language = CLanguageKind.CPP)

    assertThat(compilerSettingsCPP).hasCompilerKindWrapper()
    assertThat(compilerSettingsC).hasCompilerKindWrapper()

    assertThat(compilerSettingsCPP)
      .hasCompiler(OCCompilerId.GCC)
      .containsHeaders("iostream", "stdio.h")
      .containsSwitches("-Wall", "-DCOPTS", "-DCXXOPTS")
      .doesNotContainSwitches("-DCONLYOPTS")
      .hasDefine("SIMPLE_DEFINE", "42")
      .hasDefine("SPACE_DEFINE", "1 2 3")

    assertThat(compilerSettingsC)
      .hasCompiler(OCCompilerId.GCC)
      .containsHeaders("stdio.h")
      .containsSwitches("-Wall", "-DCOPTS", "-DCONLYOPTS")
      .doesNotContainSwitches("-DCXXOPTS")
      .hasDefine("SIMPLE_DEFINE", "42")
      .hasDefine("SPACE_DEFINE", "1 2 3")
  }

  @Test
  fun testLanguageCompilerSettings(): Unit = timeoutRunBlocking {
    // settings for a language that does not match the file's language fall back to the language specific settings
    val compilerSettingsC = project.findCompilerSetting("main/main.cc", language = CLanguageKind.C)
    val compilerSettingsCPP = project.findCompilerSetting("main/util.c", language = CLanguageKind.CPP)

    assertThat(compilerSettingsCPP)
      .hasCompiler(OCCompilerId.GCC)
      .containsSwitches("-DCXXOPTS")
      .doesNotContainSwitches("-DCONLYOPTS", "-DCOPTS")
      .hasDefine("SIMPLE_DEFINE", "42")

    assertThat(compilerSettingsC)
      .hasCompiler(OCCompilerId.GCC)
      .containsSwitches("-DCONLYOPTS")
      .doesNotContainSwitches("-DCXXOPTS", "-DCOPTS")
      .hasDefine("SIMPLE_DEFINE", "42")
  }
}
