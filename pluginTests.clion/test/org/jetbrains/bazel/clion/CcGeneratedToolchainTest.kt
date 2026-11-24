package org.jetbrains.bazel.clion

import com.intellij.testFramework.common.timeoutRunBlocking
import org.jetbrains.bazel.assertions.assertThat
import org.jetbrains.bazel.assertions.findCompilerSetting
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.jetbrains.bazel.test.framework.BazelVersions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS

@CcTestApplication
@EnabledOnOs(OS.LINUX)
class CcGeneratedToolchainTest {

  private val project by clionBazelProjectFixture("clion/generated_toolchain", buildProject = false, bazelVersion = BazelVersions.BAZEL_9)

  @Test
  fun testGlobalIncludes(): Unit = timeoutRunBlocking {
    val compilerSettings = project.findCompilerSetting("src/main.cpp")
    assertThat(compilerSettings).containsHeaders("iostream")
    assertThat(compilerSettings).containsHeaders("ostream")
  }
}
