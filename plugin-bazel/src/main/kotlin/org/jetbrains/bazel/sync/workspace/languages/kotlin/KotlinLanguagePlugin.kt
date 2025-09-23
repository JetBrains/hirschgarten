package org.jetbrains.bazel.sync.workspace.languages.kotlin

import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.server.label.label
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginContext
import org.jetbrains.bazel.sync.workspace.languages.isInternalTarget
import org.jetbrains.bazel.sync.workspace.languages.java.JavaLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.jvm.JVMPackagePrefixResolver
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import java.nio.file.Path

class KotlinLanguagePlugin(private val javaLanguagePlugin: JavaLanguagePlugin, private val bazelPathsResolver: BazelPathsResolver) :
  LanguagePlugin<KotlinBuildTarget>,
  JVMPackagePrefixResolver {
  override suspend fun createBuildTargetData(context: LanguagePluginContext, target: BspTargetInfo.TargetInfo): KotlinBuildTarget? {
    if (!target.hasKotlinTargetInfo()) {
      return null
    }
    val kotlinTarget = target.kotlinTargetInfo
    return KotlinBuildTarget(
      languageVersion = kotlinTarget.languageVersion,
      apiVersion = kotlinTarget.apiVersion,
      associates = kotlinTarget.associatesList.map { Label.parse(it) },
      kotlincOptions = kotlinTarget.toKotlincOptArguments(),
      jvmBuildTarget = javaLanguagePlugin.createBuildTargetData(context, target),
    )
  }

  // Project-level libraries: Kotlin stdlibs bundled under a synthetic label
  override fun collectProjectLevelLibraries(targets: Sequence<BspTargetInfo.TargetInfo>): Map<Label, org.jetbrains.bazel.sync.workspace.model.Library> {
    val jars = calculateProjectLevelKotlinStdlibsJars(targets)
    if (jars.isEmpty()) return emptyMap()

    val inferredSourceJars = jars
      .map { it.parent.resolve(it.fileName.toString().replace(".jar", "-sources.jar")) }
      .toSet()

    val lib = org.jetbrains.bazel.sync.workspace.model.Library(
      label = Label.synthetic("rules_kotlin_kotlin-stdlibs"),
      outputs = jars,
      sources = inferredSourceJars,
      dependencies = emptyList(),
      isLowPriority = true, // allow user-provided stdlib to override
    )
    return mapOf(lib.label to lib)
  }

  // Per-target libraries: stdlibs (as dependency edge) + kotlinc plugin jars
  override fun collectPerTargetLibraries(targets: Sequence<BspTargetInfo.TargetInfo>): Map<Label, List<org.jetbrains.bazel.sync.workspace.model.Library>> {
    val stdlibLib = collectProjectLevelLibraries(targets).values.firstOrNull()
    return targets
      .filter { it.hasKotlinTargetInfo() }
      .associate { targetInfo ->
        val kotlinInfo = targetInfo.kotlinTargetInfo
        val pluginClasspath = kotlinInfo.kotlincPluginInfosList
          .flatMap { it.pluginJarsList }
          .map { bazelPathsResolver.resolve(it) }
          .distinct()

        val pluginLibs = pluginClasspath.map { classpath ->
          org.jetbrains.bazel.sync.workspace.model.Library(
            label = Label.synthetic(classpath.fileName.toString()),
            outputs = setOf(classpath),
            sources = emptySet(),
            dependencies = emptyList(),
          )
        }

        val libs = mutableListOf<org.jetbrains.bazel.sync.workspace.model.Library>()
        if (stdlibLib != null) libs.add(stdlibLib)
        libs.addAll(pluginLibs)
        targetInfo.label() to libs
      }
  }

  private fun calculateProjectLevelKotlinStdlibsJars(targets: Sequence<BspTargetInfo.TargetInfo>): Set<Path> =
    targets
      .filter { it.hasKotlinTargetInfo() }
      .map { it.kotlinTargetInfo.stdlibsList }
      .flatMap { it.map(bazelPathsResolver::resolve) }
      .toSet()

  private fun BspTargetInfo.KotlinTargetInfo.toKotlincOptArguments(): List<String> = kotlincOptsList + additionalKotlinOpts()

  private fun BspTargetInfo.KotlinTargetInfo.additionalKotlinOpts(): List<String> =
    toKotlincPluginClasspathArguments() + toKotlincPluginOptionArguments()

  private fun BspTargetInfo.KotlinTargetInfo.toKotlincPluginOptionArguments(): List<String> =
    kotlincPluginInfosList
      .flatMap { it.kotlincPluginOptionsList }
      .flatMap { listOf("-P", "plugin:${it.pluginId}:${it.optionValue}") }

  private fun BspTargetInfo.KotlinTargetInfo.toKotlincPluginClasspathArguments(): List<String> =
    kotlincPluginInfosList
      .flatMap { it.pluginJarsList }
      .map { "-Xplugin=${bazelPathsResolver.resolve(it)}" }

  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(LanguageClass.KOTLIN)

  override fun supportsTarget(target: BspTargetInfo.TargetInfo): Boolean = target.hasKotlinTargetInfo() || target.kind in setOf(
    "kt_jvm_library", "kt_jvm_binary", "kt_jvm_test"
  )

  override fun isWorkspaceTarget(
    target: BspTargetInfo.TargetInfo,
    repoMapping: RepoMapping,
    featureFlags: FeatureFlags,
  ): Boolean {
    val internal = isInternalTarget(target.label().assumeResolved(), repoMapping)
    return internal && supportsTarget(target)
  }

  override fun collectResources(targetInfo: BspTargetInfo.TargetInfo): Sequence<Path> =
    collectResourcesWithCheck(targetInfo, bazelPathsResolver) { it.hasKotlinTargetInfo() }

  override fun resolveJvmPackagePrefix(source: Path): String? = javaLanguagePlugin.resolveJvmPackagePrefix(source)
}
