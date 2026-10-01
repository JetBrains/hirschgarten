package org.jetbrains.bazel.clion

import com.intellij.openapi.util.SystemInfoRt
import com.intellij.testFramework.common.timeoutRunBlocking
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.jetbrains.bazel.assertions.assertNotNull
import org.jetbrains.bazel.assertions.assertThat
import org.jetbrains.bazel.assertions.assertVfsLoads
import org.jetbrains.bazel.assertions.findCompiler
import org.jetbrains.bazel.assertions.findCompilerSetting
import org.jetbrains.bazel.assertions.findTarget
import org.jetbrains.bazel.assertions.findToolchain
import org.jetbrains.bazel.clion.sync.CcToolchainBuildTarget
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.jetbrains.bazel.test.framework.BazelVersionedTest
import org.jetbrains.bazel.test.framework.BazelVersions
import org.jetbrains.bazel.test.framework.annotation.EnabledOnBazel
import org.jetbrains.bsp.protocol.extractData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS

@CcTestApplication
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class CcLocalToolchainTest(override val bazelVersion: String) : BazelVersionedTest {

  @EnabledOnOs(OS.LINUX)
  class Bazel7 : CcLocalToolchainTest(BazelVersions.BAZEL_7)

  @EnabledOnOs(OS.LINUX)
  class Bazel8 : CcLocalToolchainTest(BazelVersions.BAZEL_8)

  @DisabledOnOs(OS.WINDOWS)
  class Bazel9 : CcLocalToolchainTest(BazelVersions.BAZEL_9)

  private val project by clionBazelProjectFixture("clion/simple", buildProject = true, bazelVersion = bazelVersion)

  @Test
  fun testVfsRoots() = project.assertVfsLoads()

  @Test
  fun testCompilerSettings(): Unit = timeoutRunBlocking {
    val compilerSettingsC = project.findCompilerSetting("main/util.c", language = CLanguageKind.C)
    val compilerSettingsCPP = project.findCompilerSetting("main/main.cc", language = CLanguageKind.CPP)

    assertThat(compilerSettingsCPP).hasCompilerKindWrapper()
    assertThat(compilerSettingsC).hasCompilerKindWrapper()

    assertThat(compilerSettingsCPP)
      .hasCompiler(platformDefaultCompiler())
      .containsHeaders("iostream", "stdio.h")
      .containsSwitches("-Wall", "-DCOPTS", "-DCXXOPTS")
      .doesNotContainSwitches("-DCONLYOPTS")
      .hasDefine("SIMPLE_DEFINE", "42")
      .hasDefine("SPACE_DEFINE", "1 2 3")

    assertThat(compilerSettingsC)
      .hasCompiler(platformDefaultCompiler())
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
      .hasCompiler(platformDefaultCompiler())
      .containsSwitches("-DCXXOPTS")
      .doesNotContainSwitches("-DCONLYOPTS", "-DCOPTS")
      .hasDefine("SIMPLE_DEFINE", "42")

    assertThat(compilerSettingsC)
      .hasCompiler(platformDefaultCompiler())
      .containsSwitches("-DCONLYOPTS")
      .doesNotContainSwitches("-DCXXOPTS", "-DCOPTS")
      .hasDefine("SIMPLE_DEFINE", "42")
  }

  @Test
  @EnabledOnOs(OS.MAC)
  @EnabledOnBazel(BazelVersions.BAZEL_9) // support for Bazel 8 and below will be provided by BAZEL-3585
  fun testXcodeInfo(): Unit = timeoutRunBlocking {
    val target = project.findTarget("//main:main")

    val toolchain = project.findToolchain(target)
      .assertNotNull()
      .extractData<CcToolchainBuildTarget>()
      .assertNotNull()

    assertThat(toolchain.xcodeInfo?.xcodeVersion).isNotNull().isNotBlank()
    assertThat(toolchain.xcodeInfo?.macosSdkVersion).isNotNull().isNotBlank()

    val compiler = project.findCompiler(target).assertNotNull()

    assertThat(compiler.environment).containsKey("DEVELOPER_DIR")
    assertThat(compiler.environment).containsKey("SDKROOT")
  }
}

private fun platformDefaultCompiler(): OCCompilerId {
  return when {
    SystemInfoRt.isLinux -> OCCompilerId.GCC
    SystemInfoRt.isMac -> OCCompilerId.APPLE_CLANG
    else -> fail("unsupported platform")
  }
}
