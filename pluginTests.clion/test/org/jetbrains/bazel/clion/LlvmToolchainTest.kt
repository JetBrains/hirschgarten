package org.jetbrains.bazel.clion

import com.intellij.testFramework.common.timeoutRunBlocking
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerId
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.assertions.assertThat
import org.jetbrains.bazel.assertions.assertVfsLoads
import org.jetbrains.bazel.assertions.condition
import org.jetbrains.bazel.assertions.findCompilerSetting
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.jetbrains.bazel.lookupCompilerSwitch
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS

@CcTestApplication
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisabledOnOs(OS.WINDOWS, disabledReason = "toolchains_llvm 1.7.0 does not register toolchains on Windows")
class LlvmToolchainTest {

  private val project by clionBazelProjectFixture("clion/llvm_toolchain") {
    addBuildFlags("--platforms=@toolchains_llvm//platforms:wasm32")
  }

  @Test
  fun testVfsRoots() = project.assertVfsLoads(emptyList())

  @Test
  fun testCompilerSettings(): Unit = timeoutRunBlocking {
    val compilerSettings = project.findCompilerSetting("main/main.cc")

    assertThat(compilerSettings)
      .hasCompilerKindWrapper()
      .hasCompiler(OCCompilerId.CLANG)
      .hasDefine("__llvm__", "1")
      .hasDefine("__VERSION__", condition { it.contains("Clang 19.1.0") })
      .containsHeaders("stdlib.h", "wasi/wasip2.h")

    val sysroot = compilerSettings.lookupCompilerSwitch("sysroot").single()
    assertThat(compilerSettings.compilerWorkingDir?.resolve(sysroot)).exists()
  }
}
