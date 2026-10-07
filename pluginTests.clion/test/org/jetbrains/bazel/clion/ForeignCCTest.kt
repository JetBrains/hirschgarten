package org.jetbrains.bazel.clion

import com.intellij.testFramework.common.timeoutRunBlocking
import org.jetbrains.bazel.assertions.assertThat
import org.jetbrains.bazel.assertions.findCompilerSetting
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.jetbrains.bazel.test.framework.BazelVersions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS

@CcTestApplication
@DisabledOnOs(OS.WINDOWS)
class ForeignCCTest {

  private val project by clionBazelProjectFixture("clion/foreign_cc_deps", buildProject = true, bazelVersion = BazelVersions.BAZEL_9)

  @Test
  fun testMakeApp(): Unit = timeoutRunBlocking {
    val compilerSettings = project.findCompilerSetting("make/main.c")
    assertThat(compilerSettings).containsHeaders("format_utils.h")
  }

  @Test
  fun testMakeTest(): Unit = timeoutRunBlocking {
    val compilerSettings = project.findCompilerSetting("make/test.c")
    assertThat(compilerSettings).containsHeaders("format_utils.h")
  }

  @Test
  fun testCMakeBinary(): Unit = timeoutRunBlocking {
    val compilerSettings = project.findCompilerSetting("cmake/hello.cpp")
    assertThat(compilerSettings).containsHeaders("Speaker.h")
  }
}
