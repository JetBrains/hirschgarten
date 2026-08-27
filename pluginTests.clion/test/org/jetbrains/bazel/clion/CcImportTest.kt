package org.jetbrains.bazel.clion

import com.intellij.openapi.components.service
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.SystemPropertyClassLevel
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldExist
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.clion.sync.CC_LANGUAGE_CLASS
import org.jetbrains.bazel.clion.sync.CcBuildTarget
import org.jetbrains.bazel.clion.sync.CcToolchainBuildTarget
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.sync.workspace.snapshot.hasBuildData
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.OutputLocationCollection
import org.jetbrains.bsp.protocol.extractData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@SystemPropertyClassLevel(BazelFeatureFlags.USE_PTY, "false") // otherwise tests fail due to a leaked timer
@BazelTestApplication
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CcImportTest {

  private val project by clionBazelProjectFixture("clion/simple")

  private suspend fun findTarget(label: Label): List<BuildTarget> {
    val snapshot = project.service<WorkspaceSnapshotService>().currentSnapshot()

    return snapshot.targetGraph.allTargets
      .filter { it.targetKey.label == label }
      .mapNotNull { it.load(snapshot.targets, TargetLoadOptions.ALL) }
      .toList()
  }

  private suspend fun findTarget(label: String): List<BuildTarget> {
    return findTarget(Label.parse(label))
  }

  private suspend fun findTarget(key: WorkspaceTargetKey): BuildTarget? {
    val snapshot = project.service<WorkspaceSnapshotService>().currentSnapshot()
    return snapshot.targetGraph.findTargetByKey(key, strict = true)?.load(snapshot.targets, TargetLoadOptions.ALL)
  }

  @Test
  fun testTargetsFound(): Unit = timeoutRunBlocking {
    findTarget("//main:main")
    findTarget("//main:test")
    findTarget("//lib:lib")
  }

  @Test
  fun testBinaryInfo(): Unit = timeoutRunBlocking {
    val target = findTarget("//main:main").single()
    target.kind.kind.shouldBe("cc_binary")
    target.kind.languageClasses.shouldContain(CC_LANGUAGE_CLASS)
    target.kind.ruleType.shouldBe(RuleType.BINARY)

    val data = target.extractData<CcBuildTarget>().shouldNotBeNull()

    val compilationCtx = data.compilationContext
    compilationCtx.headers.locations() shouldContain OutputLocation.Workspace("lib/lib.h")
    compilationCtx.defines.shouldContainExactly("SPACE_DEFINE=1 2 3", "SIMPLE_DEFINE=42")
    compilationCtx.includes.locations().shouldBeEmpty()
    val quoteIncludes = compilationCtx.quoteIncludes.locations()
    quoteIncludes shouldContain OutputLocation.Workspace(".")
    quoteIncludes shouldContain OutputLocation.External("rules_cc+", "")
    quoteIncludes.shouldContainBinOutput("")
    compilationCtx.systemIncludes.locations().shouldBeEmpty()

    val ruleCtx = data.ruleContext.shouldNotBeNull()
    ruleCtx.headers.locations().shouldBeEmpty()
    ruleCtx.textualHeaders.locations().shouldBeEmpty()
    ruleCtx.copts.shouldContainExactly("-Wall")
    ruleCtx.conlyopts.shouldContainExactly("-DCONLYOPTS")
    ruleCtx.cxxopts.shouldContainExactly("-DCXXOPTS")
    ruleCtx.stripIncludePrefix.shouldBe("")
    ruleCtx.includePrefix.shouldBe("")
  }

  @Test
  fun testLibraryInfo(): Unit = timeoutRunBlocking {
    val target = findTarget("//lib:lib").single()
    target.kind.kind.shouldBe("cc_library")
    target.kind.languageClasses.shouldContain(CC_LANGUAGE_CLASS)
    target.kind.ruleType.shouldBe(RuleType.LIBRARY)

    val data = target.extractData<CcBuildTarget>().shouldNotBeNull()

    val compilationCtx = data.compilationContext
    compilationCtx.headers.locations() shouldContain OutputLocation.Workspace("lib/lib.h")
    compilationCtx.defines.shouldBeEmpty()
    compilationCtx.includes.locations().shouldBeEmpty()
    val quoteIncludes = compilationCtx.quoteIncludes.locations()
    quoteIncludes shouldContain OutputLocation.Workspace(".")
    quoteIncludes.shouldContainBinOutput("")
    compilationCtx.systemIncludes.locations().shouldBeEmpty()

    val ruleCtx = data.ruleContext.shouldNotBeNull()
    ruleCtx.headers.locations() shouldContain OutputLocation.Workspace("lib/lib.h")
    ruleCtx.textualHeaders.locations().shouldBeEmpty()
    ruleCtx.copts.shouldBeEmpty()
    ruleCtx.conlyopts.shouldBeEmpty()
    ruleCtx.cxxopts.shouldBeEmpty()
    ruleCtx.stripIncludePrefix.shouldBe("")
    ruleCtx.includePrefix.shouldBe("")
  }

  @Test
  fun testTestInfo(): Unit = timeoutRunBlocking {
    val target = findTarget("//main:test").single()
    target.kind.kind.shouldBe("cc_test")
    target.kind.languageClasses.shouldContain(CC_LANGUAGE_CLASS)
    target.kind.ruleType.shouldBe(RuleType.TEST)

    val data = target.extractData<CcBuildTarget>().shouldNotBeNull()

    val compilationCtx = data.compilationContext
    compilationCtx.headers.locations() shouldContainAll
      listOf(
        OutputLocation.External("catch2+", "src/catch2/benchmark/catch_benchmark.hpp"),
        OutputLocation.External("catch2+", "src/catch2/benchmark/catch_clock.hpp"),
      )
    compilationCtx.defines.shouldBeEmpty()
    compilationCtx.includes.locations().shouldContainBinOutput("external/catch2+/_virtual_includes/catch2_generated")
    val quoteIncludes = compilationCtx.quoteIncludes.locations()
    quoteIncludes shouldContain OutputLocation.Workspace(".")
    quoteIncludes shouldContain OutputLocation.External("catch2+", "")
    quoteIncludes.shouldContainBinOutput("")
    compilationCtx.systemIncludes.locations() shouldContain OutputLocation.External("catch2+", "src")

    val ruleCtx = data.ruleContext.shouldNotBeNull()
    ruleCtx.headers.locations().shouldBeEmpty()
    ruleCtx.textualHeaders.locations().shouldBeEmpty()
    ruleCtx.copts.shouldBeEmpty()
    ruleCtx.conlyopts.shouldBeEmpty()
    ruleCtx.cxxopts.shouldBeEmpty()
    ruleCtx.stripIncludePrefix.shouldBe("")
    ruleCtx.includePrefix.shouldBe("")
  }

  @Test
  fun testToolchainInfo(): Unit = timeoutRunBlocking {
    val target = findTarget("//main:main").single()

    val toolchain = target.dependencies
      .mapNotNull { findTarget(it.targetKey) }
      .single { it.hasBuildData<CcToolchainBuildTarget>() }

    toolchain.kind.kind.shouldBe("cc_toolchain_alias")
    toolchain.kind.languageClasses.shouldContain(CC_LANGUAGE_CLASS)

    val data = toolchain.extractData<CcToolchainBuildTarget>().shouldNotBeNull()
    data.compilerName.shouldBe("false")
    data.cppOption.shouldContainAll("-D__DEFINE__", "-std=c++17")
    data.cOption.shouldContainAll("-D__DEFINE__", "-std=c17")
    data.cCompiler.shouldBe(OutputLocation.Host("/usr/bin/false"))
    data.cppCompiler.shouldBe(OutputLocation.Host("/usr/bin/false"))
    data.builtInIncludeDirectories.locations().shouldBeEmpty()
    data.sysroot.shouldBeNull()
    data.cppEnvironment.shouldContain("ENV_VARIABLE" to "ENV_VALUE")
    data.cppEnvironment.shouldContain("ENV_VARIABLE" to "ENV_VALUE")
  }
}

private fun OutputLocationCollection.locations(): List<OutputLocation> = getOutputLocations().toList()

private fun List<OutputLocation>.shouldContainBinOutput(relativePath: String) =
  shouldExist { it is OutputLocation.Output && it.root.segments.lastOrNull() == "bin" && it.relativePath == relativePath }
