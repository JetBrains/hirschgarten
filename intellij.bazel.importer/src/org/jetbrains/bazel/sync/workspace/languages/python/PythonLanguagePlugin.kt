package org.jetbrains.bazel.sync.workspace.languages.python

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.LocalRepositoryMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.info.BspTargetInfo.ArtifactLocation
import org.jetbrains.bazel.info.BspTargetInfo.PythonTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.label
import org.jetbrains.bazel.server.model.sourcesList
import org.jetbrains.bazel.sync.workspace.graph.DependencyGraph
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bsp.protocol.BazelServerFacade
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.PythonBuildTarget
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.nameWithoutExtension

internal class PythonLanguagePlugin: LanguagePlugin {
  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(LanguageClass.PYTHON)
  override fun createProjectMapper(project: Project, server: BazelServerFacade) = Mapper(server)

  class Mapper(private val server: BazelServerFacade) : LanguagePlugin.Mapper {
    private var defaultInterpreter: Path? = null
    private var defaultVersion: String? = null

    private var pyExternalSources: Map<Label, List<Path>> = emptyMap()

    override suspend fun prepareSync(
      graph: DependencyGraph,
      targetsToImport: Map<Label, TargetInfo>,
      repoMapping: RepoMapping,
    ) {
      val localRepositories = repoMapping.getLocalRepositories()
      val defaultTargetInfo = graph.idToTargetInfo.values.firstOrNull(::hasPythonInterpreter)?.pythonTargetInfo
      defaultInterpreter =
        defaultTargetInfo
          ?.interpreter
          ?.takeUnless { it.relativePath.isNullOrEmpty() }
          ?.let { server.bazelPathsResolver.resolve(it, localRepositories) }
      defaultVersion = defaultTargetInfo?.version

      pyExternalSources = graph.idToTargetInfo
        .filter { it.value.hasPythonTargetInfo() }
        .mapValues { entry ->
          getExternalSources(entry.value, localRepositories).map {
            calculateExternalSourcePath(it, localRepositories)
          }
        }
    }

    private fun hasPythonInterpreter(targetInfo: TargetInfo): Boolean =
      targetInfo.hasPythonTargetInfo() && targetInfo.pythonTargetInfo.hasInterpreter()

    override suspend fun createBuildTargetData(
      target: TargetInfo,
      targetsToImport: Map<Label, TargetInfo>,
      graph: DependencyGraph,
      repoMapping: RepoMapping,
    ): List<BuildTargetData> {
      if (!target.hasPythonTargetInfo()) {
        return emptyList()
      }

      val localRepositories = repoMapping.getLocalRepositories()
      val sourceDependencies: List<Path> = graph
        .transitiveDependenciesWithoutRootTargets(target.label())
        .flatMap { pyExternalSources[it.label()].orEmpty() }
        .distinct()

      val pythonTarget = target.pythonTargetInfo
      return listOf(
        PythonBuildTarget(
          version = pythonTarget.version.takeUnless(String::isNullOrEmpty) ?: defaultVersion,
          interpreter = calculateInterpreterPath(interpreter = pythonTarget.interpreter, localRepositories) ?: defaultInterpreter,
          imports = pythonTarget.importsList,
          generatedSources = pythonTarget.resolveGeneratedSources(repoMapping).toList(),
          sourceDependencies = sourceDependencies,
          mainFile = pythonTarget.main?.let { server.bazelPathsResolver.resolve(it, localRepositories) },
          mainModule = pythonTarget.mainModule,
        ),
      )
    }

    private fun calculateInterpreterPath(interpreter: ArtifactLocation?, localRepositories: LocalRepositoryMapping): Path? =
      interpreter
        ?.takeUnless { it.relativePath.isNullOrEmpty() }
        ?.let { server.bazelPathsResolver.resolve(it, localRepositories) }

    private fun getExternalSources(targetInfo: TargetInfo, localRepositories: LocalRepositoryMapping): List<ArtifactLocation> =
      targetInfo.sourcesList.mapNotNull { it.takeIf { server.bazelPathsResolver.isExternal(it, localRepositories) } }.toList()

    private fun calculateExternalSourcePath(externalSource: ArtifactLocation, localRepositories: LocalRepositoryMapping): Path {
      val path = server.bazelPathsResolver.resolve(externalSource, localRepositories)
      return server.bazelPathsResolver.resolve(findSitePackagesSubdirectory(path) ?: path)
    }

    private fun PythonTargetInfo.resolveGeneratedSources(repoMapping: RepoMapping): Sequence<Path> {
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
}
