package org.jetbrains.bazel.clion

import com.intellij.testFramework.common.timeoutRunBlocking
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.assertions.assertNotNull
import org.jetbrains.bazel.assertions.assertThat
import org.jetbrains.bazel.assertions.assertVfsLoads
import org.jetbrains.bazel.assertions.bazelBin
import org.jetbrains.bazel.assertions.external
import org.jetbrains.bazel.assertions.findTarget
import org.jetbrains.bazel.assertions.findToolchain
import org.jetbrains.bazel.assertions.workspace
import org.jetbrains.bazel.clion.sync.CC_LANGUAGE_CLASS
import org.jetbrains.bazel.clion.sync.CcBuildTarget
import org.jetbrains.bazel.clion.sync.CcToolchainBuildTarget
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.jetbrains.bazel.test.framework.BazelVersionedTest
import org.jetbrains.bazel.test.framework.BazelVersions
import org.jetbrains.bazel.test.framework.majorBazelVersion
import org.jetbrains.bsp.protocol.extractData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS

@CcTestApplication
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class CcImportTest(override val bazelVersion: String) : BazelVersionedTest {

  @DisabledOnOs(OS.WINDOWS)
  @CcTestApplication
  class Bazel7 : CcImportTest(BazelVersions.BAZEL_7)

  @DisabledOnOs(OS.WINDOWS)
  @CcTestApplication
  class Bazel8 : CcImportTest(BazelVersions.BAZEL_8)

  @DisabledOnOs(OS.WINDOWS)
  @CcTestApplication
  class Bazel9 : CcImportTest(BazelVersions.BAZEL_9)

  private val project by clionBazelProjectFixture("clion/simple", bazelVersion = bazelVersion) {
    addBuildFlags("--extra_toolchains=//toolchain:toolchain")
  }

  @Test
  fun testVfsRoots() = project.assertVfsLoads()

  @Test
  fun testTargetsFound(): Unit = timeoutRunBlocking {
    project.findTarget("//main:main")
    project.findTarget("//main:test")
    project.findTarget("//lib:lib")
  }

  @Test
  fun testBinaryInfo(): Unit = timeoutRunBlocking {
    val target = project.findTarget("//main:main")
    assertThat(target.kind.kind).isEqualTo("cc_binary")
    assertThat(target.kind.languageClasses).contains(CC_LANGUAGE_CLASS)
    assertThat(target.kind.ruleType).isEqualTo(RuleType.BINARY)

    val data = target.extractData<CcBuildTarget>().assertNotNull()

    val compilationCtx = data.compilationContext
    assertThat(compilationCtx.headers).haveAtLeastOne(workspace("lib/lib.h"))
    assertThat(compilationCtx.defines).containsExactly("SPACE_DEFINE=1 2 3", "SIMPLE_DEFINE=42")
    assertThat(compilationCtx.includes).isEmpty()
    assertThat(compilationCtx.systemIncludes).isEmpty()

    assertThat(compilationCtx.quoteIncludes)
      .haveAtLeastOne(workspace("."))
      .haveAtLeastOne(external("rules_cc+", ""))
      .haveAtLeastOne(bazelBin(""))

    val ruleCtx = data.ruleContext.assertNotNull()
    assertThat(ruleCtx.headers).isEmpty()
    assertThat(ruleCtx.textualHeaders).isEmpty()
    assertThat(ruleCtx.copts).containsExactly("-Wall", "-DCOPTS")
    assertThat(ruleCtx.conlyopts).containsExactly("-DCONLYOPTS")
    assertThat(ruleCtx.cxxopts).containsExactly("-DCXXOPTS")
    assertThat(ruleCtx.stripIncludePrefix).isEqualTo("")
    assertThat(ruleCtx.includePrefix).isEqualTo("")
  }

  @Test
  fun testLibraryInfo(): Unit = timeoutRunBlocking {
    val target = project.findTarget("//lib:lib")
    assertThat(target.kind.kind).isEqualTo("cc_library")
    assertThat(target.kind.languageClasses).contains(CC_LANGUAGE_CLASS)
    assertThat(target.kind.ruleType).isEqualTo(RuleType.LIBRARY)

    val data = target.extractData<CcBuildTarget>().assertNotNull()

    val compilationCtx = data.compilationContext
    assertThat(compilationCtx.headers).haveAtLeastOne(workspace("lib/lib.h"))
    assertThat(compilationCtx.defines).isEmpty()
    assertThat(compilationCtx.includes).isEmpty()
    assertThat(compilationCtx.systemIncludes).isEmpty()

    assertThat(compilationCtx.quoteIncludes)
      .haveAtLeastOne(workspace("."))
      .haveAtLeastOne(bazelBin(""))

    val ruleCtx = data.ruleContext.assertNotNull()
    assertThat(ruleCtx.headers).haveAtLeastOne(workspace("lib/lib.h"))
    assertThat(ruleCtx.textualHeaders).isEmpty()
    assertThat(ruleCtx.copts).isEmpty()
    assertThat(ruleCtx.conlyopts).isEmpty()
    assertThat(ruleCtx.cxxopts).isEmpty()
    assertThat(ruleCtx.stripIncludePrefix).isEqualTo("")
    assertThat(ruleCtx.includePrefix).isEqualTo("")
  }

  @Test
  fun testTestInfo(): Unit = timeoutRunBlocking {
    val target = project.findTarget("//main:test")
    assertThat(target.kind.kind).isEqualTo("cc_test")
    assertThat(target.kind.languageClasses).contains(CC_LANGUAGE_CLASS)
    assertThat(target.kind.ruleType).isEqualTo(RuleType.TEST)

    val data = target.extractData<CcBuildTarget>().assertNotNull()

    val compilationCtx = data.compilationContext
    assertThat(compilationCtx.headers).haveAtLeastOne(external("catch2+", "src/catch2/benchmark/catch_benchmark.hpp"))
    assertThat(compilationCtx.defines).isEmpty()
    assertThat(compilationCtx.includes).haveAtLeastOne(bazelBin("external/catch2+/_virtual_includes/catch2_generated"))

    // the builtin rules treat includes slightly different
    if (majorBazelVersion <= 8) {
      assertThat(compilationCtx.systemIncludes).haveAtLeastOne(external("catch2+", "src"))
    } else {
      assertThat(compilationCtx.systemIncludes).isEmpty()
    }

    assertThat(compilationCtx.quoteIncludes)
      .haveAtLeastOne(workspace("."))
      .haveAtLeastOne(external("catch2+", ""))
      .haveAtLeastOne(bazelBin(""))

    val ruleCtx = data.ruleContext.assertNotNull()
    assertThat(ruleCtx.headers).isEmpty()
    assertThat(ruleCtx.textualHeaders).isEmpty()
    assertThat(ruleCtx.copts).isEmpty()
    assertThat(ruleCtx.conlyopts).isEmpty()
    assertThat(ruleCtx.cxxopts).isEmpty()
    assertThat(ruleCtx.stripIncludePrefix).isEqualTo("")
    assertThat(ruleCtx.includePrefix).isEqualTo("")
  }

  @Test
  fun testToolchainInfo(): Unit = timeoutRunBlocking {
    val target = project.findTarget("//main:main")

    val toolchain = project.findToolchain(target).single()

    // only with toolchains aspects enabled can we discover the actual toolchain
    val toolchainKind = if (majorBazelVersion <= 8)  "cc_toolchain_alias" else "cc_toolchain"

    assertThat(toolchain.kind.kind).isEqualTo(toolchainKind)
    assertThat(toolchain.kind.languageClasses).contains(CC_LANGUAGE_CLASS)

    val data = toolchain.extractData<CcToolchainBuildTarget>().assertNotNull()
    assertThat(data.compilerName).isEqualTo("false")
    assertThat(data.cppOption).contains("-D__DEFINE__", "-std=c++17")
    assertThat(data.cOption).contains("-D__DEFINE__", "-std=c17")
    assertThat(data.cCompiler).isHost("/usr/bin/false")
    assertThat(data.cppCompiler).isHost("/usr/bin/false")
    assertThat(data.builtInIncludeDirectories).isEmpty()
    assertThat(data.sysroot).isNull()
    assertThat(data.cppEnvironment).containsEntry("ENV_VARIABLE", "ENV_VALUE")
  }
}
