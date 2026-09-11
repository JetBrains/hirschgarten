package org.jetbrains.bazel.fixtures

import com.intellij.util.containers.MultiMap
import com.jetbrains.cidr.lang.workspace.compiler.GCCCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerKind
import org.jetbrains.bazel.clion.sync.CC_LANGUAGE_CLASS
import org.jetbrains.bazel.clion.sync.CcBuildTarget
import org.jetbrains.bazel.clion.sync.CcToolchainBuildTarget
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.DependencyLabelKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.persistence.InMemoryWorkspaceTargetMap
import org.jetbrains.bazel.sync.workspace.snapshot.OutputLocationCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.SourceFileCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetGraphBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.sync.workspace.snapshot.hasBuildData
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.OutputLocationCollection
import org.jetbrains.bsp.protocol.extractData
import java.nio.file.Path

/**
 * Declares a small C and C++ target graph and builds a [WorkspaceSnapshot] from it.
 *
 * Use it to test one import step without a Bazel run or a project. Every setter
 * of the DSL is a function. A setter that takes a vararg appends, and a setter that
 * takes one value replaces.
 *
 * ```
 * val snapshot = ccProject {
 *   val toolchain = ccToolchain { label("//toolchain:toolchain") }
 *
 *   ccBinary {
 *     label("//main:main")
 *     srcs("main/main.cc")
 *     copts("-Wall")
 *     deps(toolchain)
 *   }
 * }
 * ```
 */
internal fun ccProject(workspaceRoot: Path = Path.of("/workspace"), declare: CcProjectBuilder.() -> Unit): WorkspaceSnapshot {
  return CcProjectBuilder(workspaceRoot).apply(declare).build()
}

internal class CcProjectBuilder(private val workspaceRoot: Path) {

  private val targets = mutableListOf<BuildTarget>()

  private val defaultToolchain: BuildTarget by lazy { ccToolchain { } }

  fun ccLibrary(declare: CcTargetBuilder.() -> Unit): BuildTarget = ccTarget("cc_library", RuleType.LIBRARY, declare)

  fun ccBinary(declare: CcTargetBuilder.() -> Unit): BuildTarget = ccTarget("cc_binary", RuleType.BINARY, declare)

  fun ccTest(declare: CcTargetBuilder.() -> Unit): BuildTarget = ccTarget("cc_test", RuleType.TEST, declare)

  private fun ccTarget(kind: String, ruleType: RuleType, declare: CcTargetBuilder.() -> Unit): BuildTarget {
    return CcTargetBuilder(kind, ruleType).apply(declare).build(workspaceRoot, ::defaultToolchain).also(targets::add)
  }

  fun ccToolchain(declare: CcToolchainBuilder.() -> Unit): BuildTarget {
    return CcToolchainBuilder().apply(declare).build(workspaceRoot).also(targets::add)
  }

  fun plainTarget(declare: PlainTargetBuilder.() -> Unit): BuildTarget {
    return PlainTargetBuilder().apply(declare).build(workspaceRoot).also(targets::add)
  }

  fun build(): WorkspaceSnapshot {
    val targetsByKey = targets.associateBy { it.key }

    return WorkspaceSnapshot.EMPTY.copy(
      targets = InMemoryWorkspaceTargetMap(targetsByKey),
      targetGraph = WorkspaceTargetGraphBuilder.build(targetsByKey.keys, targets),
    )
  }
}

internal class CcTargetBuilder(private val kind: String, private val ruleType: RuleType) {

  private var label: String? = null
  private var configurationId: String? = null

  private val srcs = mutableListOf<String>()
  private val hdrs = mutableListOf<String>()
  private val copts = mutableListOf<String>()
  private val conlyopts = mutableListOf<String>()
  private val cxxopts = mutableListOf<String>()
  private val defines = mutableListOf<String>()
  private val includes = mutableListOf<String>()
  private val quoteIncludes = mutableListOf<String>()
  private val systemIncludes = mutableListOf<String>()
  private val deps = MultiMap<DependencyLabelKind, BuildTarget>()
  private var useDefaultToolchain = true

  fun label(label: String) {
    this.label = label
  }

  fun configurationId(configurationId: String) {
    this.configurationId = configurationId
  }

  fun srcs(vararg paths: String) {
    srcs += paths
  }

  /** Declares a header. It reaches both the rule context and the compilation context. */
  fun hdrs(vararg paths: String) {
    hdrs += paths
  }

  fun copts(vararg options: String) {
    copts += options
  }

  fun conlyopts(vararg options: String) {
    conlyopts += options
  }

  fun cxxopts(vararg options: String) {
    cxxopts += options
  }

  fun defines(vararg defines: String) {
    this.defines += defines
  }

  fun includes(vararg paths: String) {
    includes += paths
  }

  fun quoteIncludes(vararg paths: String) {
    quoteIncludes += paths
  }

  fun systemIncludes(vararg paths: String) {
    systemIncludes += paths
  }

  fun deps(vararg targets: BuildTarget) {
    deps(DependencyLabelKind.COMPILE, *targets)
  }

  fun deps(kind: DependencyLabelKind, vararg targets: BuildTarget) {
    deps.putValues(kind, targets.toList())
  }

  fun noToolchain() {
    useDefaultToolchain = false
  }

  internal fun build(root: Path, defaultToolchain: () -> BuildTarget): BuildTarget {
    val label = requireNotNull(label)

    if (useDefaultToolchain && deps.values().none { it.hasBuildData<CcToolchainBuildTarget>() }) {
      deps(DependencyLabelKind.TOOLCHAIN, defaultToolchain())
    }

    val allHeaders = hdrs.map(OutputLocation::Workspace).toMutableList<OutputLocation>()
    val allIncludes = includes.map(OutputLocation::parseExecrootPath).toMutableList()
    val allQuoteIncludes = quoteIncludes.map(OutputLocation::parseExecrootPath).toMutableList()
    val allSystemIncludes = systemIncludes.map(OutputLocation::parseExecrootPath).toMutableList()

    for (dep in deps.values()) {
      dep.extractData<CcBuildTarget>()?.let { data ->
        allHeaders.addAll(data.compilationContext.headers.getOutputLocations())
        allIncludes.addAll(data.compilationContext.includes.getOutputLocations())
        allQuoteIncludes.addAll(data.compilationContext.quoteIncludes.getOutputLocations())
        allSystemIncludes.addAll(data.compilationContext.systemIncludes.getOutputLocations())
      }
      dep.extractData<CcToolchainBuildTarget>()?.let { data ->
        allSystemIncludes.addAll(data.builtInIncludeDirectories.getOutputLocations())
      }
    }

    allIncludes.add(OutputLocation.Workspace(packagePathOf(label)))

    return TestBuildTarget(
      key = targetKey(label, configurationId),
      kind = TargetKind(kind = kind, languageClasses = setOf(CC_LANGUAGE_CLASS), ruleType = ruleType),
      dependencies = deps.entrySet().flatMap { it.value.map { target -> DependencyLabel(target.key, it.key) } },
      baseDirectory = root.resolve(packagePathOf(label)),
      sources = SourceFileCollectionBuilder.build(
        relativeRoot = Path.of(packagePathOf(label)),
        paths = srcs.map(root::resolve),
      ),
      data = listOf(
        CcBuildTarget(
          ruleContext = CcBuildTarget.RuleContext(
            headers = locations(hdrs.map(OutputLocation::Workspace)),
            textualHeaders = OutputLocationCollection.EMPTY,
            copts = copts,
            conlyopts = conlyopts,
            cxxopts = cxxopts,
            args = emptyList(),
            includePrefix = "",
            stripIncludePrefix = "",
          ),
          compilationContext = CcBuildTarget.CompilationContext(
            headers = locations(allHeaders),
            defines = defines.toList(),
            includes = locations(allIncludes),
            quoteIncludes = locations(allQuoteIncludes),
            systemIncludes = locations(allSystemIncludes),
          ),
        ),
      ),
    )
  }
}

internal class CcToolchainBuilder {

  private var label: String = "//toolchain:toolchain"
  private var configurationId: String? = null
  private var compilerName: String = "gcc"
  private var cCompiler: String = "/usr/bin/gcc"
  private var cppCompiler: String = "/usr/bin/g++"
  private var compilerKind: OCCompilerKind = GCCCompilerKind
  private var sysroot: String? = null

  private val cOptions = mutableListOf<String>()
  private val cppOptions = mutableListOf<String>()
  private val builtinIncludes = mutableListOf<String>()
  private val env = LinkedHashMap<String, String>()

  fun label(label: String) {
    this.label = label
  }

  fun configurationId(configurationId: String) {
    this.configurationId = configurationId
  }

  fun compilerName(compilerName: String) {
    this.compilerName = compilerName
  }

  fun compiler(path: String) {
    cCompiler = path
    cppCompiler = path
  }

  fun compilerKind(compilerKind: OCCompilerKind) {
    this.compilerKind = compilerKind
  }

  fun sysroot(path: String) {
    sysroot = path
  }

  fun cOptions(vararg options: String) {
    cOptions += options
  }

  fun cppOptions(vararg options: String) {
    cppOptions += options
  }

  fun builtinIncludes(vararg paths: String) {
    builtinIncludes += paths
  }

  /** Sets both the C and the C++ environment. */
  fun env(vararg entries: Pair<String, String>) {
    env.putAll(entries)
  }

  internal fun build(root: Path): BuildTarget = TestBuildTarget(
    key = targetKey(label, configurationId),
    kind = TargetKind(kind = "cc_toolchain_alias", languageClasses = setOf(CC_LANGUAGE_CLASS), ruleType = RuleType.UNKNOWN),
    baseDirectory = root.resolve(packagePathOf(label)),
    data = listOf(
      CcToolchainBuildTarget(
        targetName = "toolchain",
        compilerName = compilerName,
        cppOption = cppOptions,
        cOption = cOptions,
        cCompiler = OutputLocation.parseExecrootPath(cCompiler),
        cppCompiler = OutputLocation.parseExecrootPath(cppCompiler),
        builtInIncludeDirectories = locations(builtinIncludes.map(OutputLocation::Workspace)),
        sysroot = sysroot?.let(OutputLocation::parseExecrootPath),
        cEnvironment = env,
        cppEnvironment = env,
      ),
    ),
  )
}

internal class PlainTargetBuilder {

  private var label: String? = null
  private var configurationId: String? = null
  private val deps = mutableListOf<DependencyLabel>()

  fun label(label: String) {
    this.label = label
  }

  fun configurationId(configurationId: String) {
    this.configurationId = configurationId
  }

  fun deps(vararg targets: WorkspaceTargetKey) {
    deps(DependencyLabelKind.COMPILE, *targets)
  }

  fun deps(kind: DependencyLabelKind, vararg targets: WorkspaceTargetKey) {
    deps += targets.map { DependencyLabel(targetKey = it, kind = kind) }
  }

  internal fun build(root: Path): BuildTarget {
    val label = requireNotNull(this.label)

    return TestBuildTarget(
      key = targetKey(label, configurationId),
      kind = TargetKind(kind = "plain", languageClasses = setOf(), ruleType = RuleType.UNKNOWN),
      baseDirectory = root.resolve(packagePathOf(label)),
    )
  }
}

private fun targetKey(label: String, configurationId: String?): WorkspaceTargetKey {
  return WorkspaceTargetKey(Label.parse(label), WorkspaceConfigurationId.of(configurationId))
}

private fun locations(paths: Collection<OutputLocation>): OutputLocationCollection {
  return OutputLocationCollectionBuilder.ofLocations(paths.toList())
}

private fun packagePathOf(label: String): String {
  return label.substringAfter("//").substringBefore(':')
}
