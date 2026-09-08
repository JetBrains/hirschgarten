package org.jetbrains.bazel.clion

import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.SystemPropertyClassLevel
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerId
import org.jetbrains.bazel.assertions.findCompilerSettings
import org.jetbrains.bazel.assertions.shouldContainDefines
import org.jetbrains.bazel.assertions.shouldContainHeaders
import org.jetbrains.bazel.assertions.shouldContainSwitches
import org.jetbrains.bazel.assertions.shouldHaveCompiler
import org.jetbrains.bazel.assertions.shouldNotContainSwitches
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
    val compilerSettingsC = project.findCompilerSettings("main/main.cc", language = CLanguageKind.C)
    val compilerSettingsCPP = project.findCompilerSettings("main/main.cc", language = CLanguageKind.CPP)

    compilerSettingsCPP.shouldHaveCompiler(OCCompilerId.GCC)
    compilerSettingsC.shouldHaveCompiler(OCCompilerId.GCC)

    compilerSettingsCPP.shouldContainHeaders("iostream")

    compilerSettingsCPP.shouldContainHeaders("stdio.h")
    compilerSettingsC.shouldContainHeaders("stdio.h")

    compilerSettingsCPP.shouldContainSwitches("-Wall", "-DCXXOPTS");
    compilerSettingsCPP.shouldNotContainSwitches("-DCONLYOPTS");
    compilerSettingsC.shouldContainSwitches("-Wall", "-DCONLYOPTS");
    compilerSettingsC.shouldNotContainSwitches("-DCXXOPTS");

    compilerSettingsCPP.shouldContainDefines("SIMPLE_DEFINE=42", "SPACE_DEFINE=1 2 3")
    compilerSettingsC.shouldContainDefines("SIMPLE_DEFINE=42", "SPACE_DEFINE=1 2 3")
  }
}
