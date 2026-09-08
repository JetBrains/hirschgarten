package org.jetbrains.bazel.clion

import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.SystemPropertyClassLevel
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerId
import org.jetbrains.bazel.assertions.assertThat
import org.jetbrains.bazel.assertions.findCompilerSetting
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@SystemPropertyClassLevel(BazelFeatureFlags.USE_PTY, "false")
@BazelTestApplication
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CcLocalToolchainTest {

  private val project by clionBazelProjectFixture("clion/simple")

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
