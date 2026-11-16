package org.jetbrains.bazel.clion

import com.intellij.testFramework.common.timeoutRunBlocking
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerId
import org.jetbrains.bazel.assertions.assertThat
import org.jetbrains.bazel.assertions.assertVfsLoads
import org.jetbrains.bazel.assertions.findCompilerSetting
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.clionBazelArchiveProjectFixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@CcTestApplication
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WorkerdTest {

  private val project by clionBazelArchiveProjectFixture("@cc_test_project_cloudflare_workerd//:archive", waitForSymbols = false, buildProject = true)

  @Test
  fun testVfsRoots() = project.assertVfsLoads()

  @Test
  fun testActorCache(): Unit = timeoutRunBlocking {
    val compilerSettings = project.findCompilerSetting("src/workerd/io/actor-cache.c++")

    assertThat(compilerSettings)
      .hasCompiler(OCCompilerId.CLANG)
      .containsSwitches("-Wall", "-Wextra", "-DCHROMIUM_ZLIB_NO_CHROMECONF")
      .containsWorkspaceHeader("workerd/io/actor-storage.h", project)
      .containsHeaders("kj/debug.h", "algorithm")
  }
}
