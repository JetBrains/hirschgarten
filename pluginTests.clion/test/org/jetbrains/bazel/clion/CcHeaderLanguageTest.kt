package org.jetbrains.bazel.clion

import com.intellij.testFramework.common.timeoutRunBlocking
import com.jetbrains.cidr.lang.CLanguageKind
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.assertions.assertNotNull
import org.jetbrains.bazel.assertions.assertThat
import org.jetbrains.bazel.assertions.assertVfsLoads
import org.jetbrains.bazel.assertions.findResolveConfiguration
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.jetbrains.bazel.resolveCompilerSwitches
import org.jetbrains.bazel.test.framework.BazelVersions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS

@CcTestApplication
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisabledOnOs(OS.WINDOWS)
class CcHeaderLanguageTest {

  private val project by clionBazelProjectFixture("clion/header_language", buildProject = true, bazelVersion = BazelVersions.BAZEL_9)

  @Test
  fun testVfsRoots() = project.assertVfsLoads()

  @Test
  fun testCSourceHeaderOnCppTarget(): Unit = timeoutRunBlocking {
    val file = project.rootDir.findFileByRelativePath("cpp/ryu.c").assertNotNull()

    val configuration = project.findResolveConfiguration("cpp/ryu.c")
    assertThat(configuration.getDeclaredLanguageKind(file)).isEqualTo(CLanguageKind.CPP)

    val cSettings = configuration.getCompilerSettings(CLanguageKind.C, file)
    assertThat(cSettings).doesNotContainSwitches("-std=c++17")

    val cppSettings = configuration.getCompilerSettings(CLanguageKind.CPP, file)
    assertThat(cppSettings).containsSwitches("-std=c++17")
  }

  @Test
  fun testCppSourceHeaderOnCTarget(): Unit = timeoutRunBlocking {
    val file = project.rootDir.findFileByRelativePath("c/lib.h").assertNotNull()

    val configuration = project.findResolveConfiguration("c/lib.h")
    assertThat(configuration.getDeclaredLanguageKind(file)).isEqualTo(CLanguageKind.C)

    val cSettings = configuration.getCompilerSettings(CLanguageKind.C, file)
    assertThat(cSettings).containsSwitches("-std=c99")

    val cppSettings = configuration.getCompilerSettings(CLanguageKind.CPP, file)
    assertThat(cppSettings).doesNotContainSwitches("-std=c99")
  }
}
