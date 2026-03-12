package org.jetbrains.bazel.sync.workspace.languages.python

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.LocalRepositoryMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.info.BspTargetInfo.ArtifactLocation
import org.jetbrains.bazel.info.BspTargetInfo.PythonTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.label.label
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginContext
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.PythonBuildTarget
import java.nio.file.Path

internal class PythonLanguagePlugin(private val bazelPathsResolver: BazelPathsResolver, private val workspaceContext: WorkspaceContext) : LanguagePlugin<PythonBuildTarget> {
  private var defaultInterpreter: Path? = null
  private var defaultVersion: String? = null

  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(LanguageClass.PYTHON)

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
    val isCodeGenerator = target.sourcesList.isEmpty() && workspaceContext.pythonCodeGeneratorRuleNames.contains(target.kind)
    return PythonBuildTarget(
      version = pythonTarget.version.takeUnless(String::isNullOrEmpty) ?: defaultVersion,
      interpreter = calculateInterpreterPath(interpreter = pythonTarget.interpreter, localRepositories) ?: defaultInterpreter,
      imports = pythonTarget.importsList,
      isCodeGenerator = isCodeGenerator,
      generatedSources = if (isCodeGenerator) pythonTarget.generatedSourcesList.mapNotNull { bazelPathsResolver.resolve(it, localRepositories) } else emptyList(),
      sourceDependencies = sourceDependencies,
      mainFile = pythonTarget.main?.let { bazelPathsResolver.resolve(it, localRepositories) },
      mainModule = pythonTarget.mainModule
    )
  }

  private fun calculateInterpreterPath(interpreter: ArtifactLocation?, localRepositories : LocalRepositoryMapping): Path? =
    interpreter
      ?.takeUnless { it.relativePath.isNullOrEmpty() }
      ?.let { bazelPathsResolver.resolve(it, localRepositories) }

  private fun getExternalSources(targetInfo: TargetInfo, localRepositories : LocalRepositoryMapping): List<ArtifactLocation> =
    targetInfo.sourcesList.mapNotNull { it.takeIf { bazelPathsResolver.isExternal(it, localRepositories) }}

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
