package com.intellij.bazel.python.backend.sync

import com.google.devtools.intellij.aspect.Common.ArtifactLocation
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.LocalRepositoryMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.label.label
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.python.lang.PythonBuildTarget
import org.jetbrains.bazel.python.lang.PythonLanguageClass
import org.jetbrains.bazel.server.model.sourcesList
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.snapshot.SourceFileCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSyncConfig
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bsp.protocol.BuildTargetData
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.nameWithoutExtension
import kotlin.reflect.KClass

internal class PythonLanguagePlugin : LanguagePlugin {
  override val providedBuildTargetTypes: Set<KClass<out BuildTargetData>>
    get() = setOf(PythonBuildTarget::class)

  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(PythonLanguageClass.PYTHON)
  override fun collectUsedLanguages(target: TargetIdeInfo): List<LanguageClass> {
    if (target.hasPythonTargetInfo())
      return listOf(PythonLanguageClass.PYTHON)
    return emptyList()
  }
  override suspend fun createSyncConfigs(project: Project, projectView: ProjectView): List<WorkspaceSyncConfig> {
    val config = PythonWorkspaceSyncConfig(
      isPythonSupportEnabled = BazelFeatureFlags.isPythonSupportEnabled,
    )
    return listOf(config)
  }

  override suspend fun mapBuildTargetData(
    server: BazelServerFacade,
    target: TargetIdeInfo,
    repoMapping: RepoMapping,
  ): List<BuildTargetData> {
    if (!target.hasPythonTargetInfo()) {
      return emptyList()
    }
    val baseDirectory = server.bazelPathsResolver.toDirectoryPath(target.label().assumeResolved(), repoMapping)
    val localRepositories = repoMapping.getLocalRepositories()
    val pythonTarget = target.pythonTargetInfo
    val runnerScript =
      if (target.hasExecutableInfo()) {
        server.bazelPathsResolver.resolve(target.executableInfo.executableFile, localRepositories)
      }
      else {
        null
      }
    return listOf(
      PythonBuildTarget(
        version = pythonTarget.version.takeUnless(String::isNullOrEmpty),
        interpreter = calculateInterpreterPath(server, interpreter = pythonTarget.interpreter, localRepositories),
        imports = pythonTarget.importsList,
        generatedSources = SourceFileCollectionBuilder.build(
          relativeRoot = baseDirectory,
          paths = pythonTarget.resolveGeneratedSources(server, repoMapping),
        ),
        externalSources = getExternalSources(server, target, localRepositories)
          .map { calculateExternalSourcePath(server, it, localRepositories) }
          .let { SourceFileCollectionBuilder.build(relativeRoot = baseDirectory, paths = it) },
        mainFile = MainSourceFinder.findMainFile(target, pythonTarget, server.bazelPathsResolver, localRepositories),
        mainModule = pythonTarget.mainModule,
        runnerScript = runnerScript,
      ),
    )
  }

  private fun calculateInterpreterPath(server: BazelServerFacade, interpreter: ArtifactLocation?, localRepositories: LocalRepositoryMapping): Path? =
    interpreter
      ?.takeUnless { it.relativePath.isNullOrEmpty() }
      ?.let { server.bazelPathsResolver.resolve(it, localRepositories) }

  private fun getExternalSources(server: BazelServerFacade, targetInfo: TargetIdeInfo, localRepositories: LocalRepositoryMapping): List<ArtifactLocation> =
    targetInfo.sourcesList.mapNotNull { it.takeIf { server.bazelPathsResolver.isExternal(it, localRepositories) } }.toList()

  private fun calculateExternalSourcePath(server: BazelServerFacade, externalSource: ArtifactLocation, localRepositories: LocalRepositoryMapping): Path {
    val path = server.bazelPathsResolver.resolve(externalSource, localRepositories)
    return server.bazelPathsResolver.resolve(findSitePackagesSubdirectory(path) ?: path)
  }

  private fun IntellijIdeInfo.PythonTargetInfo.resolveGeneratedSources(server: BazelServerFacade, repoMapping: RepoMapping): Sequence<Path> {
    val localRepositories = repoMapping.getLocalRepositories()
    return generatedSourcesList
      .asSequence()
      .flatMap { location ->
        val sourceFile = server.bazelPathsResolver.resolve(location, localRepositories)
        // some code gen rules return directories. we need to figure out what files are there
        if (sourceFile.isDirectory()) {
          Files
            .walk(sourceFile)
            .toList()
        }
        else {
          listOf(sourceFile)
        }
      }
      .filter { it.extension == "py" || it.extension == "pyw" }
      .map { sourceFile ->
        // If type annotation exists - use it instead of generated .py file
        // https://peps.python.org/pep-0484/#the-type-of-class-objects
        if (sourceFile.extension == "py") {
          val interfaceStub = sourceFile.parent.resolve("${sourceFile.nameWithoutExtension}.pyi")
          if (interfaceStub.exists())
            return@map interfaceStub
        }

        sourceFile
      }
  }

  private tailrec fun findSitePackagesSubdirectory(path: Path?): Path? =
    when {
      path == null -> null
      // PyNames.SITE_PACKAGES
      path.endsWith("site-packages") -> path
      else -> findSitePackagesSubdirectory(path.parent)
    }
}
