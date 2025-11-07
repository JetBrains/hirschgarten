package org.jetbrains.bazel.sync.workspace.languages.java

import com.intellij.openapi.project.Project
import com.intellij.util.EnvironmentUtil
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.info.BspTargetInfo.JvmTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.sync.workspace.languages.JvmPackageResolver
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginContext
import org.jetbrains.bazel.sync.workspace.languages.java.source_root.JavaSourceRootPackageInference
import org.jetbrains.bazel.sync.workspace.languages.java.source_root.prefix.SourcePatternEval
import org.jetbrains.bazel.sync.workspace.languages.java.source_root.prefix.JavaSourceRootPatternContributor
import org.jetbrains.bazel.sync.workspace.languages.java.source_root.prefix.SourceRootPattern
import org.jetbrains.bazel.sync.workspace.languages.java.source_root.projectview.javaSROEnable
import org.jetbrains.bazel.sync.workspace.languages.jvm.JVMPackagePrefixResolver
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import java.nio.file.Path

class JavaLanguagePlugin(
  private val bazelPathsResolver: BazelPathsResolver,
  private val jdkResolver: JdkResolver,
  private val packageResolver: JvmPackageResolver,
) : LanguagePlugin<JvmBuildTarget>,
    JVMPackagePrefixResolver {
  private val packageInference = JavaSourceRootPackageInference(packageResolver)
  private var jdk: Jdk? = null

  private var cachedJavaSROEnable = false
  private var cachedSROIncludeMatchers: List<SourceRootPattern> = listOf()
  private var cachedSROExcludeMatchers: List<SourceRootPattern> = listOf()

  override fun prepareSync(project: Project, targets: Sequence<TargetInfo>, workspaceContext: WorkspaceContext) {
    val ideJavaHomeOverride = workspaceContext.ideJavaHomeOverride
    jdk = ideJavaHomeOverride?.let { Jdk(javaHome = it) } ?: jdkResolver.resolve(targets)

    val projectView = ProjectViewService.getInstance(project)
      .getCachedProjectView()
    cachedJavaSROEnable = projectView.javaSROEnable
    cachedSROIncludeMatchers = JavaSourceRootPatternContributor.ep
      .extensionList
      .flatMap { it.getIncludePatterns(project) }
      .toList()
    cachedSROExcludeMatchers = JavaSourceRootPatternContributor.ep
      .extensionList
      .flatMap { it.getExcludePatterns(project) }
      .toList()
  }

  override suspend fun createBuildTargetData(context: LanguagePluginContext, target: TargetInfo): JvmBuildTarget? {
    if (!target.hasJvmTargetInfo()) {
      return null
    }
    val jvmTarget = target.jvmTargetInfo
    val binaryOutputs = jvmTarget.jarsList.flatMap { it.binaryJarsList }.map(bazelPathsResolver::resolve)
    val mainClass = getMainClass(jvmTarget)

    val jdk = jdk ?: return null
    val javaVersion = javaVersionFromJavacOpts(jvmTarget.javacOptsList) ?: javaVersionFromToolchain(target)
    val javaHome = jdk.javaHome
    val environmentVariables =
      context.target.envMap + context.target.envInheritList.associateWith { EnvironmentUtil.getValue(it) ?: "" }
    return JvmBuildTarget(
      javaVersion = javaVersion.orEmpty(),
      javaHome = javaHome,
      javacOpts = jvmTarget.javacOptsList,
      binaryOutputs = binaryOutputs,
      environmentVariables = environmentVariables,
      mainClass = mainClass,
      jvmArgs = jvmTarget.jvmFlagsList,
      programArgs = jvmTarget.argsList,
    )
  }

  override fun transformSources(sources: List<SourceItem>): List<SourceItem> {
    if (cachedJavaSROEnable) {
      val root = bazelPathsResolver.workspaceRoot()
      val (matched, unmatched) = SourcePatternEval.evalSources(
        workspaceRoot = root,
        sources = sources,
        includes = cachedSROIncludeMatchers,
        excludes = cachedSROExcludeMatchers,
      )
      if (matched.isNotEmpty()) {
        packageInference.inferPackages(matched)
      }
      for (item in unmatched) {
        item.jvmPackagePrefix = packageResolver.calculateJvmPackagePrefix(item.path)
      }
    } else {
      for (item in sources) {
        item.jvmPackagePrefix = packageResolver.calculateJvmPackagePrefix(item.path)
      }
    }
    return sources
  }

  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(LanguageClass.JAVA)

  override fun resolveJvmPackagePrefix(source: Path): String? = packageResolver.calculateJvmPackagePrefix(source)

  private fun getMainClass(jvmTargetInfo: JvmTargetInfo): String? = jvmTargetInfo.mainClass.takeUnless { jvmTargetInfo.mainClass.isBlank() }

  private fun javaVersionFromToolchain(target: TargetInfo): String? = if (target.hasJavaToolchainInfo()) {
    target.javaToolchainInfo.sourceVersion
  } else {
    null
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
}
