package org.jetbrains.bazel.clion

import com.intellij.testFramework.common.timeoutRunBlocking
import org.jetbrains.bazel.assertions.assertThat
import org.jetbrains.bazel.assertions.assertVfsLoads
import org.jetbrains.bazel.assertions.findCompilerSetting
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS

@CcTestApplication
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisabledOnOs(OS.WINDOWS)
class HardlinkIncludeDirsTest {

  private val project by clionBazelProjectFixture("clion/hardlink_include_dirs", buildProject = true)

  @Test
  fun testVfsRoots() = project.assertVfsLoads(emptyList())

  @Test
  fun checkResolveConfiguration(): Unit = timeoutRunBlocking {
    val compilerSettings = project.findCompilerSetting("app/smoke.cc")
    assertThat(compilerSettings).containsHeaders("fakelib/fakelib.h")
    assertThat(compilerSettings).containsCachedHeaders("fakelib/generated.h", project, symlink = false)
    assertThat(compilerSettings).containsCachedHeaders("config/answer.h", project, symlink = false)
    assertThat(compilerSettings).containsCachedHeaders("config/version.h", project, symlink = false)
  }
}
