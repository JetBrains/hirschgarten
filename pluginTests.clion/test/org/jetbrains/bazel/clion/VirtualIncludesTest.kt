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
import org.assertj.core.api.Assertions.assertThat as assertThatGeneric

@CcTestApplication
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VirtualIncludesTest {

  private val project by clionBazelProjectFixture("clion/virtual_includes", buildProject = true)

  @Test
  @DisabledOnOs(OS.WINDOWS, disabledReason = "copts aren't supported yet for MSVC")
  fun testVfsRoots() = project.assertVfsLoads(allowedRoots = emptyList())

  @Test
  fun checkIncludes(): Unit = timeoutRunBlocking {
    val compilerSettings = project.findCompilerSetting("main/main.cc")

    assertThat(compilerSettings)
      .containsHeaders("strip_absolut/strip_absolut.h")
      .containsCachedHeaders("strip_absolut/strip_absolut.h", project, symlink = true)

    assertThat(compilerSettings)
      .containsHeaders("strip_absolut/generated.h")
      .containsCachedHeaders("strip_absolut/generated.h", project, symlink = false)

    assertThat(compilerSettings)
      .containsHeaders("strip_relative.h")
      .containsCachedHeaders("strip_relative.h", project, symlink = true)

    assertThat(compilerSettings)
      .containsHeaders("raw_default.h")
      .containsWorkspaceHeader("raw_default.h", project)

    assertThat(compilerSettings)
      .containsHeaders("raw_system.h")
      .containsWorkspaceHeader("raw_system.h", project)

    assertThat(compilerSettings)
      .containsHeaders("raw_quote.h")
      .containsWorkspaceHeader("raw_quote.h", project)

    assertThat(compilerSettings)
      .containsHeaders("external/generated.h")
      .containsCachedHeaders("external/generated.h", project, symlink = false)

    assertThat(compilerSettings)
      .containsHeaders("lib/generated.h")
      .containsCachedHeaders("lib/generated.h", project, symlink = false)
  }

  @Test
  @DisabledOnOs(OS.WINDOWS, disabledReason = "copts aren't supported yet for MSVC")
  fun checkCoptIncludes(): Unit = timeoutRunBlocking {
    val compilerSettings = project.findCompilerSetting("main/raw.cc")

    assertThat(compilerSettings)
      .containsHeaders("raw_default.h")
      .containsWorkspaceHeader("raw_default.h", project)

    assertThat(compilerSettings)
      .containsHeaders("raw_system.h")
      .containsWorkspaceHeader("raw_system.h", project)

    assertThat(compilerSettings)
      .containsHeaders("raw_quote.h")
      .containsWorkspaceHeader("raw_quote.h", project)
  }

  @Test
  fun checkImplDeps(): Unit = timeoutRunBlocking {
    val compilerSettings = project.findCompilerSetting("lib/impl_deps/impl.cc")

    assertThatGeneric(compilerSettings.headersSearchRoots.allRoots).isNotEmpty()

    assertThat(compilerSettings)
      .containsHeaders("strip_relative.h")
      .containsCachedHeaders("strip_relative.h", project, symlink = true)
  }
}
