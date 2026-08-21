package org.jetbrains.bazel.clion

import com.intellij.openapi.components.service
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.SystemPropertyClassLevel
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.clion.sync.CC_LANGUAGE_CLASS
import org.jetbrains.bazel.clion.sync.CcBuildTarget
import org.jetbrains.bazel.clion.sync.CcToolchainBuildTarget
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.matcher.shouldBeExecutionRootPath
import org.jetbrains.bazel.matcher.shouldContainArtifact
import org.jetbrains.bazel.matcher.shouldContainArtifacts
import org.jetbrains.bazel.matcher.shouldContainExecutionRootPaths
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.sync.workspace.snapshot.hasBuildData
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.extractData
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.time.Duration.Companion.minutes

@BazelTestApplication
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SystemPropertyClassLevel(BazelFeatureFlags.USE_PTY, "false") // otherwise tests fail due to a leaked timer
class CcImportTest {

  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  @BeforeAll
  fun setup(): Unit = timeoutRunBlocking(timeout = 5.minutes) {
    // note: loads test projects from testData sibling module
    fixture.copyBazelTestProject("clion/simple")
    fixture.performBazelSync()
  }

  private suspend fun findTarget(label: Label): List<BuildTarget> {
    val snapshot = fixture.project.service<WorkspaceSnapshotService>().currentSnapshot()

    return snapshot.targetGraph.allTargets
      .filter { it.targetKey.label == label }
      .mapNotNull { it.load(snapshot.targets, TargetLoadOptions.ALL) }
      .toList()
  }

  private suspend fun findTarget(label: String): List<BuildTarget> {
    return findTarget(Label.parse(label))
  }

  private suspend fun findTarget(key: WorkspaceTargetKey): BuildTarget? {
    val snapshot = fixture.project.service<WorkspaceSnapshotService>().currentSnapshot()
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
    compilationCtx.headers.shouldContainArtifacts("lib/lib.h")
    compilationCtx.defines.shouldContainExactly("SPACE_DEFINE=1 2 3", "SIMPLE_DEFINE=42")
    compilationCtx.includes.shouldBeEmpty()
    compilationCtx.quoteIncludes.shouldContainExecutionRootPaths(".", "bazel-bin", "external/rules_cc+")
    compilationCtx.systemIncludes.shouldBeEmpty()

    val ruleCtx = data.ruleContext.shouldNotBeNull()
    ruleCtx.headers.shouldBeEmpty()
    ruleCtx.textualHeaders.shouldBeEmpty()
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
    compilationCtx.headers.shouldContainArtifact("lib/lib.h")
    compilationCtx.defines.shouldBeEmpty()
    compilationCtx.includes.shouldBeEmpty()
    compilationCtx.quoteIncludes.shouldContainExecutionRootPaths(".", "bazel-bin")
    compilationCtx.systemIncludes.shouldBeEmpty()

    val ruleCtx = data.ruleContext.shouldNotBeNull()
    ruleCtx.headers.shouldContainArtifact("lib/lib.h")
    ruleCtx.textualHeaders.shouldBeEmpty()
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
    compilationCtx.headers.shouldContainArtifact("src/catch2/benchmark/catch_benchmark.hpp", "external/catch2+", isExternal = true)
    compilationCtx.headers.shouldContainArtifact("src/catch2/benchmark/catch_clock.hpp", "external/catch2+", isExternal = true)
    compilationCtx.defines.shouldBeEmpty()
    compilationCtx.includes.shouldContainExecutionRootPaths("bazel-bin/external/catch2+/_virtual_includes/catch2_generated")
    compilationCtx.quoteIncludes.shouldContainExecutionRootPaths(".", "bazel-bin", "external/catch2+")
    compilationCtx.systemIncludes.shouldContainExecutionRootPaths("external/catch2+/src")

    val ruleCtx = data.ruleContext.shouldNotBeNull()
    ruleCtx.headers.shouldBeEmpty()
    ruleCtx.textualHeaders.shouldBeEmpty()
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
    data.cCompiler.shouldBeExecutionRootPath("/usr/bin/false")
    data.cppCompiler.shouldBeExecutionRootPath("/usr/bin/false")
    data.builtInIncludeDirectories.shouldBeEmpty()
    data.sysroot.shouldBeExecutionRootPath("")
    data.cppEnvironment.shouldContain("ENV_VARIABLE" to "ENV_VALUE")
    data.cppEnvironment.shouldContain("ENV_VARIABLE" to "ENV_VALUE")
  }
}
