package org.jetbrains.bazel.sync.workspace.languages.kotlin

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.LocalRepositoryMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.languages.java.JvmLanguagePluginMixin
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.sync.workspace.snapshot.toWorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.bsp.protocol.LibraryItem
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.notExists
import kotlin.reflect.KClass

@ApiStatus.Internal
class KotlinLanguagePlugin : JvmLanguagePluginMixin {
  override val providedBuildTargetTypes: Set<KClass<out BuildTargetData>>
    get() = setOf(KotlinBuildTarget::class)

  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(LanguageClass.KOTLIN)
  override fun collectUsedLanguages(target: TargetIdeInfo): List<LanguageClass> {
    if (target.hasKotlinTargetInfo()) {
      return listOf(LanguageClass.KOTLIN)
    }
    return emptyList()
  }
  override fun createProjectMapper(project: Project, server: BazelServerFacade): Mapper = Mapper(server)

  class Mapper(private val server: BazelServerFacade) : JvmLanguagePluginMixin.Mapper {

    override suspend fun createBuildTargetData(
      target: TargetIdeInfo,
      targetsToImport: Map<WorkspaceTargetKey, TargetIdeInfo>,
      repoMapping: RepoMapping,
    ): List<BuildTargetData> {
      if (!target.hasKotlinTargetInfo()) {
        return emptyList()
      }
      val kotlinTarget = target.kotlinTargetInfo
      val localRepositories = repoMapping.getLocalRepositories()
      val targetKey = target.key.toWorkspaceTargetKey()
      return listOf(
        KotlinBuildTarget(
          languageVersion = kotlinTarget.languageVersion.takeIf { it.isNotBlank() },
          apiVersion = kotlinTarget.apiVersion.takeIf { it.isNotBlank() },
          associates = kotlinTarget.associatesList.map { targetKey.copy(label = Label.parse(it)) },
          moduleName = kotlinTarget.moduleName.takeIf { it.isNotBlank() },
          kotlincOptions = kotlinTarget.toKotlincOptArguments(localRepositories),
        ),
      )
    }

    private fun IntellijIdeInfo.KotlinTargetInfo.toKotlincOptArguments(localRepositories: LocalRepositoryMapping): List<String> =
      kotlincOptsList + additionalKotlinOpts(localRepositories)

    private fun IntellijIdeInfo.KotlinTargetInfo.additionalKotlinOpts(localRepositories: LocalRepositoryMapping): List<String> =
      toKotlincPluginClasspathArguments(localRepositories) + toKotlincPluginOptionArguments()

    private fun IntellijIdeInfo.KotlinTargetInfo.toKotlincPluginOptionArguments(): List<String> =
      kotlincPluginInfosList
        .flatMap { it.kotlincPluginOptionsList }
        .flatMap { listOf("-P", "plugin:${it.pluginId}:${it.optionValue}") }

    private fun IntellijIdeInfo.KotlinTargetInfo.toKotlincPluginClasspathArguments(localRepositories: LocalRepositoryMapping): List<String> =
      kotlincPluginInfosList
        .flatMap { it.pluginJarsList }
        .map { "-Xplugin=${server.bazelPathsResolver.resolve(it, localRepositories)}" }

    override suspend fun toolchainLibraries(
      targetsToImport: Map<WorkspaceTargetKey, TargetIdeInfo>,
      repoMapping: RepoMapping,
    ): Map<WorkspaceTargetKey, List<LibraryItem>> {
      val projectLevelKotlinStdlib = calculateProjectLevelKotlinStdlib(targetsToImport.values, repoMapping)
                                     ?: return emptyMap()
      val kotlinTargetsIds = targetsToImport.filter { it.value.hasKotlinTargetInfo() }.map { it.key }
      return kotlinTargetsIds.associateWith { listOf(projectLevelKotlinStdlib) }
    }

    private suspend fun calculateProjectLevelKotlinStdlib(
      targetsToImport: Collection<TargetIdeInfo>,
      repoMapping: RepoMapping,
    ): LibraryItem? {
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
          key = WorkspaceTargetKey(label = Label.synthetic("rules_kotlin_kotlin-stdlibs")),
          jars = server.outFileHardLinks.createOutputFileHardLinks(kotlinStdlibJars),
          sourceJars = server.outFileHardLinks.createOutputFileHardLinks(inferredSourceJars),
          ijars = emptyList(),
          mavenCoordinates = null,
          containsInternalJars = false,
        )
      }
      else {
        null
      }
    }

    private fun calculateProjectLevelKotlinStdlibJars(targetsToImport: Collection<TargetIdeInfo>, repoMapping: RepoMapping): List<Path> {
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
