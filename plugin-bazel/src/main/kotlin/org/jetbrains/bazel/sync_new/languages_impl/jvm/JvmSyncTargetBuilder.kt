package org.jetbrains.bazel.sync_new.languages_impl.jvm

import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bazel.sync.workspace.languages.DefaultJvmPackageResolver
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.JavaSourceRootPackageInference
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.prefix.JavaSourceRootPatternContributor
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.prefix.SourcePatternEval
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.prefix.SourceRootPattern
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.projectview.javaSROEnable
import org.jetbrains.bazel.sync_new.bridge.LegacySyncTargetInfo
import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.graph.impl.BazelPath
import org.jetbrains.bazel.sync_new.graph.impl.PRIORITY_NORMAL
import org.jetbrains.bazel.sync_new.graph.impl.getIncompletePath
import org.jetbrains.bazel.sync_new.graph.impl.resolve
import org.jetbrains.bazel.sync_new.lang.SyncLanguageDataBuilder
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.JvmPrefixSourceFile
import java.nio.file.Path
import kotlin.io.path.extension

class JvmSyncTargetBuilder : SyncLanguageDataBuilder<JvmSyncTargetData> {

  private val packageResolver = DefaultJvmPackageResolver()
  private var cachedJavaSROEnable = false
  private var cachedSROIncludeMatchers: List<SourceRootPattern> = listOf()
  private var cachedSROExcludeMatchers: List<SourceRootPattern> = listOf()
  private lateinit var workspaceContext: WorkspaceContext
  private lateinit var bazelPathsResolver: BazelPathsResolver

  override suspend fun init(ctx: SyncContext) {
    val project = ctx.project
    val projectView = ProjectViewService.getInstance(project)
      .getCachedProjectView()
    cachedJavaSROEnable = projectView.javaSROEnable
    val patterns = JavaSourceRootPatternContributor.ep
      .extensionList
      .map { it.getPatterns(project) }
    cachedSROIncludeMatchers = patterns.flatMap { it.includes }
    cachedSROExcludeMatchers = patterns.flatMap { it.excludes }
    workspaceContext = project.connection.runWithServer { server -> server.workspaceContext() }
    bazelPathsResolver = project.connection.runWithServer { server -> server.workspaceBazelPaths().bazelPathsResolver }
  }

  override suspend fun buildTargetData(
      ctx: SyncContext,
      target: LegacySyncTargetInfo,
  ): JvmSyncTargetData? {
    val target = target.target
    if (!target.hasJvmTargetInfo()) {
      return null
    }
    val jvmTargetInfo = target.jvmTargetInfo
    val jvmTarget = JvmTarget(
      sources = resolveSources(target)
        .also { runSourceRootOptimizer(it) },
      generatedSources = target.generatedSourcesList.map { BazelPath.fromFileLocation(it) },
      jdeps = jvmTargetInfo.jdepsList
        .map { BazelPath.fromFileLocation(it) },
      hasApiGeneratingPlugin = jvmTargetInfo.hasApiGeneratingPlugins,
    )
    val compilerOptions = JvmCompilerOptions(
      javaVersion = javaVersionFromJavacOpts(jvmTargetInfo.javacOptsList),
      javacOpts = jvmTargetInfo.javacOptsList,
      javaHome = getJavaHome(target),
    )
    return JvmSyncTargetData(
      jvmTarget = jvmTarget,
      priority = PRIORITY_NORMAL,
      compilerOptions = compilerOptions,
      outputs = jvmTargetInfo.jarsList.toJvmOutputs(),
      generatedOutputs = jvmTargetInfo.generatedJarsList.toJvmOutputs(),
      binaryMainClass = jvmTargetInfo.mainClass.takeUnless { jvmTargetInfo.mainClass.isBlank() },
      toolchain = resolveToolchain(target),
    )
  }

  private fun List<BspTargetInfo.JvmOutputs>.toJvmOutputs(): JvmOutputs {
    return JvmOutputs(
      classJars = flatMap { it.binaryJarsList }.map { BazelPath.fromFileLocation(it) },
      srcJars = flatMap { it.sourceJarsList }.map { BazelPath.fromFileLocation(it) },
      iJars = flatMap { it.interfaceJarsList }.map { BazelPath.fromFileLocation(it) },
    )
  }

  private fun resolveSources(target: BspTargetInfo.TargetInfo): List<JvmSourceFile> {
    val sources =
      target.sourcesList
        .asSequence()
        .map(BazelPath::fromFileLocation)
        .map {
          JvmSourceFile(
            path = it,
            priority = PRIORITY_NORMAL,
            generated = false,
          )
        }
    val generatedSources =
      target.generatedSourcesList
        .asSequence()
        .map(BazelPath::fromFileLocation)
        .filter { it.getIncompletePath().extension != "srcjar" }
        .map {
          JvmSourceFile(
            path = it,
            priority = PRIORITY_NORMAL,
            generated = true,
          )
        }
    return (sources + generatedSources).toList()
  }

  private fun runSourceRootOptimizer(sources: List<JvmSourceFile>) {
    val adaptedSources = sources.map { JvmPrefixSourceFileAdapter(it) }
    if (cachedJavaSROEnable) {
      val root = bazelPathsResolver.workspaceRoot()
      val (matched, unmatched) = SourcePatternEval.evalSources(
        workspaceRoot = root,
        sources = adaptedSources,
        includes = cachedSROIncludeMatchers,
        excludes = cachedSROExcludeMatchers,
      )
      if (matched.isNotEmpty()) {
        JavaSourceRootPackageInference(packageResolver).inferPackages(matched)
      }
      for (item in unmatched) {
        item.jvmPackagePrefix = packageResolver.calculateJvmPackagePrefix(item.path)
      }
    } else {
      for (item in adaptedSources) {
        item.jvmPackagePrefix = packageResolver.calculateJvmPackagePrefix(item.path)
      }
    }
  }

  private inner class JvmPrefixSourceFileAdapter(val source: JvmSourceFile) : JvmPrefixSourceFile {
    override val path: Path by lazy { bazelPathsResolver.resolve(source.path) }
    override var jvmPackagePrefix: String?
      get() = source.jvmPackagePrefix
      set(value) {
        source.jvmPackagePrefix = value
      }

  }

  private fun javaVersionFromJavacOpts(javacOpts: List<String>): String? {
    for (i in javacOpts.indices) {
      val option = javacOpts[i]
      val flagName = option.substringBefore(' ', missingDelimiterValue = option)
      val argument = option.substringAfter(' ', missingDelimiterValue = "")
      if (flagName == "-target" || flagName == "--target" || flagName == "--release") {
        if (argument.isNotBlank()) return argument
        return javacOpts.getOrNull(i + 1)
      }
    }
    return null
  }

  private fun getJavaHome(target: BspTargetInfo.TargetInfo): Path? {
    if (target.hasJavaToolchainInfo()) {
      return null
    }
    val javaToolchainInfo = target.javaToolchainInfo
    return if (javaToolchainInfo.hasBootClasspathJavaHome()) {
      bazelPathsResolver.resolve(javaToolchainInfo.bootClasspathJavaHome)
    } else if (javaToolchainInfo.hasJavaHome()) {
      bazelPathsResolver.resolve(javaToolchainInfo.javaHome)
    } else {
      null
    }
  }

  private fun resolveToolchain(target: BspTargetInfo.TargetInfo): JvmToolchain? {
    return if (target.hasJavaToolchainInfo() && (target.javaToolchainInfo.hasBootClasspathJavaHome() || !target.hasJavaRuntimeInfo())) {
      val javaToolchainInfo = target.javaToolchainInfo
      val javaHome = if (javaToolchainInfo.hasBootClasspathJavaHome()) {
        javaToolchainInfo.bootClasspathJavaHome
      } else if (javaToolchainInfo.hasJavaHome()) {
        javaToolchainInfo.javaHome
      } else {
        null
      }

      JvmToolchain(
        kind = if (javaToolchainInfo.hasBootClasspathJavaHome()) JvmToolchainKind.BOOT_CLASSPATH else JvmToolchainKind.TOOLCHAIN,
        javaHome = javaHome?.let(bazelPathsResolver::resolve) ?: return null,
      )
    } else if (target.hasJavaRuntimeInfo()) {
      val javaRuntimeInfo = target.javaRuntimeInfo
      val javaHome = if (javaRuntimeInfo.hasJavaHome()) javaRuntimeInfo.javaHome else null

      JvmToolchain(
        kind = JvmToolchainKind.RUNTIME,
        javaHome = javaHome?.let(bazelPathsResolver::resolve) ?: return null,
      )
    } else {
      null
    }
  }
}
