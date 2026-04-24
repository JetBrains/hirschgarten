package org.jetbrains.bazel.sync.workspace.languages.kotlin

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.LocalRepositoryMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.createOutputFileHardLinks
import org.jetbrains.bazel.sync.workspace.graph.DependencyGraph
import org.jetbrains.bazel.sync.workspace.languages.java.JvmLanguagePluginMixin
import org.jetbrains.bsp.protocol.BazelServerFacade
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.bsp.protocol.LibraryItem
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.notExists

@ApiStatus.Internal
class KotlinLanguagePlugin: JvmLanguagePluginMixin {
  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(LanguageClass.KOTLIN)
  override fun createProjectMapper(project: Project, server: BazelServerFacade) = Mapper(server)

  class Mapper(private val server: BazelServerFacade) : JvmLanguagePluginMixin.Mapper {
    override suspend fun createBuildTargetData(
      target: TargetInfo,
      targetsToImport: Map<Label, TargetInfo>,
      graph: DependencyGraph,
      repoMapping: RepoMapping,
    ): List<BuildTargetData> {
      if (!target.hasKotlinTargetInfo()) {
        return emptyList()
      }
      val kotlinTarget = target.kotlinTargetInfo
      val localRepositories = repoMapping.getLocalRepositories()
      return listOf(
        KotlinBuildTarget(
          languageVersion = kotlinTarget.languageVersion.takeIf { it.isNotBlank() },
          apiVersion = kotlinTarget.apiVersion.takeIf { it.isNotBlank() },
          associates = kotlinTarget.associatesList.map { Label.parse(it) },
          moduleName = kotlinTarget.moduleName.takeIf { it.isNotBlank() },
          kotlincOptions = kotlinTarget.toKotlincOptArguments(localRepositories),
        ),
      )
    }

    private fun BspTargetInfo.KotlinTargetInfo.toKotlincOptArguments(localRepositories: LocalRepositoryMapping): List<String> =
      kotlincOptsList + additionalKotlinOpts(localRepositories)

    private fun BspTargetInfo.KotlinTargetInfo.additionalKotlinOpts(localRepositories: LocalRepositoryMapping): List<String> =
      toKotlincPluginClasspathArguments(localRepositories) + toKotlincPluginOptionArguments()

    private fun BspTargetInfo.KotlinTargetInfo.toKotlincPluginOptionArguments(): List<String> =
      kotlincPluginInfosList
        .flatMap { it.kotlincPluginOptionsList }
        .flatMap { listOf("-P", "plugin:${it.pluginId}:${it.optionValue}") }

    private fun BspTargetInfo.KotlinTargetInfo.toKotlincPluginClasspathArguments(localRepositories: LocalRepositoryMapping): List<String> =
      kotlincPluginInfosList
        .flatMap { it.pluginJarsList }
        .map { "-Xplugin=${server.bazelPathsResolver.resolve(it, localRepositories)}" }

    override suspend fun toolchainLibraries(
      targetsToImport: Map<Label, TargetInfo>,
      repoMapping: RepoMapping,
    ): Map<Label, List<LibraryItem>> {
      val projectLevelKotlinStdlib = calculateProjectLevelKotlinStdlib(targetsToImport.values, repoMapping)
                                     ?: return emptyMap()
      val kotlinTargetsIds = targetsToImport.filter { it.value.hasKotlinTargetInfo() }.map { it.key }
      return kotlinTargetsIds.associateWith { listOf(projectLevelKotlinStdlib) }
    }

    private suspend fun calculateProjectLevelKotlinStdlib(targetsToImport: Collection<TargetInfo>, repoMapping: RepoMapping): LibraryItem? {
      val kotlinStdlibJars = calculateProjectLevelKotlinStdlibJars(targetsToImport, repoMapping)

      // rules_kotlin does not expose source jars for jvm stdlibs, so this is the way they can be retrieved for now
      val inferredSourceJars =
        kotlinStdlibJars
          .map { it.parent.resolve(it.fileName.toString().replace(".jar", "-sources.jar")) }
          .filter { it.exists() }
          .onEach {
            if (it.notExists())
              logger.warn("target [kotlin-stdlib]: $it does not exist.")
          }

      return if (kotlinStdlibJars.isNotEmpty()) {
        LibraryItem(
          id = Label.synthetic("rules_kotlin_kotlin-stdlibs"),
          dependencies = emptyList(),
          jars = server.outFileHardLinks.createOutputFileHardLinks(kotlinStdlibJars),
          sourceJars = server.outFileHardLinks.createOutputFileHardLinks(inferredSourceJars),
          ijars = emptyList(),
          mavenCoordinates = null,
          containsInternalJars = false,
        )
      } else {
        null
      }
    }

    private fun calculateProjectLevelKotlinStdlibJars(targetsToImport: Collection<TargetInfo>, repoMapping: RepoMapping): List<Path> {
      val localRepositories = repoMapping.getLocalRepositories()
      return targetsToImport
        .filter { it.hasKotlinTargetInfo() }
        .flatMap { it.kotlinTargetInfo.stdlibsList }
        .map { server.bazelPathsResolver.resolve(it, localRepositories) }
        .distinct()
    }
  }

  companion object {
    private val logger = logger<KotlinLanguagePlugin>()
  }
}
