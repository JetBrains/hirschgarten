package org.jetbrains.bazel.sync.workspace.languages.kotlin

import com.google.devtools.intellij.aspect.Common.ArtifactLocation
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.LocalRepositoryMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.sync.JavaLanguageClass
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.jvm.KotlinBuildTarget
import org.jetbrains.bazel.sync.workspace.snapshot.OutputLocationCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.toWorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTargetData
import kotlin.io.path.exists
import kotlin.reflect.KClass

@ApiStatus.Internal
class KotlinLanguagePlugin : LanguagePlugin {
  override val providedBuildTargetTypes: Set<KClass<out BuildTargetData>>
    get() = setOf(KotlinBuildTarget::class)

  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(JavaLanguageClass.KOTLIN)

  override fun collectUsedLanguages(target: TargetIdeInfo): List<LanguageClass> {
    if (target.hasKotlinTargetInfo()) {
      return listOf(JavaLanguageClass.KOTLIN)
    }
    return emptyList()
  }


  override suspend fun mapBuildTargetData(
    server: BazelServerFacade,
    target: TargetIdeInfo,
    repoMapping: RepoMapping,
  ): List<BuildTargetData> {
    if (!target.hasKotlinTargetInfo()) {
      return emptyList()
    }
    val kotlinTarget = target.kotlinTargetInfo
    val localRepositories = repoMapping.getLocalRepositories()

    val ktStdlibJars = ArrayList<ArtifactLocation>()
    val ktStdlibSources = ArrayList<ArtifactLocation>()
    for (output in kotlinTarget.stdlibJarsList.orEmpty()) {
      val jars = output.binaryJarsList
      val sources =
        output.sourceJarsList.takeIf { it.isNotEmpty() } ?: run {
          // fallback to hack, awaits proper fix in rules_kotlin
          // https://github.com/bazel-contrib/rules_kotlin/pull/1761
          jars.map { it.toSourcesJar() }
            .filter { server.bazelPathsResolver.resolve(it, localRepositories).exists() }
        }

      ktStdlibJars.addAll(jars)
      ktStdlibSources.addAll(sources)
    }

    val kspSourceJars = if (target.hasJvmTargetInfo()) {
      val target = target.javaCommon
      target.generatedJarsList.asSequence()
        .flatMap { it.sourceJarsList }
        .filter { it.isKspSourceJar() && !server.bazelPathsResolver.resolve(it, localRepositories).startsWith(server.bazelInfo.workspaceRoot) }
        .toList()
    }
    else {
      listOf()
    }
    return listOf(
      KotlinBuildTarget(
        languageVersion = kotlinTarget.languageVersion.takeIf { it.isNotBlank() },
        apiVersion = kotlinTarget.apiVersion.takeIf { it.isNotBlank() },
        associates = kotlinTarget.associatedTargetsList.map { it.toWorkspaceTargetKey() },
        moduleName = kotlinTarget.moduleName.takeIf { it.isNotBlank() },
        kotlincOptions = kotlinTarget.toKotlincOptArguments(server, localRepositories).toList(),
        stdlibJars = OutputLocationCollectionBuilder.build(ktStdlibJars, server.outputParser),
        stdlibInferredSourceJars = OutputLocationCollectionBuilder.build(ktStdlibSources, server.outputParser),
        exportedCompilerPluginTargetsList = kotlinTarget.exportedCompilerPluginTargetsList.map { it.toWorkspaceTargetKey() },
        kspSourceJars = OutputLocationCollectionBuilder.build(kspSourceJars, server.outputParser),
      ),
    )
  }

  private fun IntellijIdeInfo.KotlinTargetInfo.toKotlincOptArguments(
    server: BazelServerFacade,
    localRepositories: LocalRepositoryMapping,
  ): List<String> =
    kotlincOptsList + additionalKotlinOpts(server, localRepositories)

  private fun IntellijIdeInfo.KotlinTargetInfo.additionalKotlinOpts(
    server: BazelServerFacade,
    localRepositories: LocalRepositoryMapping,
  ): List<String> =
    toKotlincPluginClasspathArguments(server, localRepositories) + toKotlincPluginOptionArguments()

  private fun IntellijIdeInfo.KotlinTargetInfo.toKotlincPluginOptionArguments(): List<String> =
    kotlincPluginInfosList
      .flatMap { it.kotlincPluginOptionsList }
      .flatMap { listOf("-P", "plugin:${it.pluginId}:${it.optionValue}") }

  private fun IntellijIdeInfo.KotlinTargetInfo.toKotlincPluginClasspathArguments(
    server: BazelServerFacade,
    localRepositories: LocalRepositoryMapping,
  ): List<String> =
    kotlincPluginInfosList
      .flatMap { it.pluginJarsList }
      .map { "-Xplugin=${server.bazelPathsResolver.resolve(it, localRepositories)}" }

  // KSP inside rules_kotlin is special, rules_kotlin/intellij-aspect doesn't distinguish between
  // normal compiler outputs and KSP ones, that's why we have to use this heuristic.
  // Ideally `KtJvmInfo` provider should expose something like ksp_srcjars for plugin.
  private fun ArtifactLocation.isKspSourceJar(): Boolean =
    relativePath.endsWith("ksp-gensrc.jar")

  // the `-sources.jar` sibling of a jar
  private fun ArtifactLocation.toSourcesJar(): ArtifactLocation {
    val directory = relativePath.substringBeforeLast('/', missingDelimiterValue = "")
    val fileName = relativePath.substringAfterLast('/').replace(".jar", "-sources.jar")
    return toBuilder().setRelativePath(if (directory.isEmpty()) fileName else "$directory/$fileName").build()
  }
}
