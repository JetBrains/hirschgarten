package org.jetbrains.bazel.sync.workspace.languages.python

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.LocalRepositoryMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.info.BspTargetInfo.ArtifactLocation
import org.jetbrains.bazel.info.BspTargetInfo.PythonTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.label.label
import org.jetbrains.bazel.server.model.sourcesList
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginContext
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.PythonBuildTarget
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.nameWithoutExtension

internal class PythonLanguagePlugin(private val bazelPathsResolver: BazelPathsResolver) : LanguagePlugin<PythonBuildTarget> {
  private var defaultInterpreter: Path? = null
  private var defaultVersion: String? = null

  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(LanguageClass.PYTHON)

  private fun PythonTargetInfo.resolveGeneratedSources(repoMapping: RepoMapping): Sequence<Path> {
    val localRepositories = repoMapping.getLocalRepositories()
    return generatedSourcesList
      .asSequence()
      .flatMap { location ->
        val sourceFile = bazelPathsResolver.resolve(location, localRepositories)
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

  override fun prepareSync(project: Project, targets: Map<Label, TargetInfo>, workspaceContext: WorkspaceContext, repoMapping: RepoMapping) {
    val localRepositories = repoMapping.getLocalRepositories()
    val defaultTargetInfo = calculateDefaultTargetInfo(targets.values.asSequence())
    defaultInterpreter =
      defaultTargetInfo
        ?.interpreter
        ?.takeUnless { it.relativePath.isNullOrEmpty() }
        ?.let { bazelPathsResolver.resolve(it, localRepositories) }
    defaultVersion = defaultTargetInfo?.version
  }

  private fun calculateDefaultTargetInfo(targets: Sequence<TargetInfo>): PythonTargetInfo? =
    targets
      .filter(::hasPythonInterpreter)
      .firstOrNull()
      ?.pythonTargetInfo

  private fun hasPythonInterpreter(targetInfo: TargetInfo): Boolean =
    targetInfo.hasPythonTargetInfo() && targetInfo.pythonTargetInfo.hasInterpreter()

  override suspend fun createBuildTargetData(context: LanguagePluginContext, target: TargetInfo, repoMapping: RepoMapping): PythonBuildTarget? {
    if (!target.hasPythonTargetInfo()) {
      return null
    }
    val localRepositories = repoMapping.getLocalRepositories()
    val sourceDependencies =
      if (context.target.hasPythonTargetInfo()) {
        context.graph
          .transitiveDependenciesWithoutRootTargets(context.target.label())
          .flatMap { getExternalSources(it, localRepositories) }
          .map { calculateExternalSourcePath(it, localRepositories) }
          .distinct()
          .toList()
      } else {
        emptyList()
      }

    val pythonTarget = target.pythonTargetInfo
    return PythonBuildTarget(
      version = pythonTarget.version.takeUnless(String::isNullOrEmpty) ?: defaultVersion,
      interpreter = calculateInterpreterPath(interpreter = pythonTarget.interpreter, localRepositories) ?: defaultInterpreter,
      imports = pythonTarget.importsList,
      generatedSources = pythonTarget.resolveGeneratedSources(repoMapping).toList(),
      sourceDependencies = sourceDependencies,
      mainFile = pythonTarget.main?.let { bazelPathsResolver.resolve(it, localRepositories) },
      mainModule = pythonTarget.mainModule,
    )
  }

  private fun calculateInterpreterPath(interpreter: ArtifactLocation?, localRepositories : LocalRepositoryMapping): Path? =
    interpreter
      ?.takeUnless { it.relativePath.isNullOrEmpty() }
      ?.let { bazelPathsResolver.resolve(it, localRepositories) }

  private fun getExternalSources(targetInfo: TargetInfo, localRepositories : LocalRepositoryMapping): List<ArtifactLocation> =
    targetInfo.sourcesList.mapNotNull { it.takeIf { bazelPathsResolver.isExternal(it, localRepositories) }}.toList()

  private fun calculateExternalSourcePath(externalSource: ArtifactLocation, localRepositories : LocalRepositoryMapping): Path {
    val path = bazelPathsResolver.resolve(externalSource, localRepositories)
    return bazelPathsResolver.resolve(findSitePackagesSubdirectory(path) ?: path)
  }

  private tailrec fun findSitePackagesSubdirectory(path: Path?): Path? =
    when {
      path == null -> null
      // PyNames.SITE_PACKAGES
      path.endsWith("site-packages") -> path
      else -> findSitePackagesSubdirectory(path.parent)
    }
}
