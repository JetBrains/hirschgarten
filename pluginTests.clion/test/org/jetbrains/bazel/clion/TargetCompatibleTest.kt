package org.jetbrains.bazel.clion

import com.intellij.openapi.util.SystemInfoRt
import com.intellij.testFramework.common.timeoutRunBlocking
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.assertions.assertVfsLoads
import org.jetbrains.bazel.assertions.findCompilerSettings
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@CcTestApplication
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TargetCompatibleTest {

  private val project by clionBazelProjectFixture("clion/target_compatible")

  @Test
  fun testVfsRoots() = project.assertVfsLoads(emptyList())

  @Test
  fun testCompilerSettings(): Unit = timeoutRunBlocking {
    assertThat(project.findCompilerSettings("main/linux.cc")).hasSize(if (SystemInfoRt.isLinux) 1 else 0)
    assertThat(project.findCompilerSettings("main/macos.cc")).hasSize(if (SystemInfoRt.isMac) 1 else 0)
    assertThat(project.findCompilerSettings("main/windows.cc")).hasSize(if (SystemInfoRt.isWindows) 1 else 0)
  }
}
