package org.jetbrains.bazel.clion

import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.SystemPropertyClassLevel
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerId
import org.jetbrains.bazel.assertions.assertThat
import org.jetbrains.bazel.assertions.findCompilerSettings
import org.jetbrains.bazel.assertions.shouldContainSwitches
import org.jetbrains.bazel.assertions.shouldHaveCompiler
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@SystemPropertyClassLevel(BazelFeatureFlags.USE_PTY, "false")
@BazelTestApplication
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArmToolchainTest {

  private val project by clionBazelProjectFixture("clion/arm_toolchain") {
    deriveTargetsFromDirectories(false)
    addTargets("//:main_u575")
  }

  @Test
  fun testCompilerSettings(): Unit = timeoutRunBlocking {
    val compilerSettings = project.findCompilerSettings("srcs/main.c", CLanguageKind.C)

    compilerSettings.shouldHaveCompiler(OCCompilerId.GCC)
    compilerSettings.shouldContainSwitches("-mcpu=cortex-m33", "-mthumb", "-DSTM32U575xx")
  }
}
