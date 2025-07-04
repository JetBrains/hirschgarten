package org.jetbrains.bazel.server.sync.languages.python

import org.jetbrains.bazel.info.FileLocation
import org.jetbrains.bazel.info.PythonTargetInfo
import org.jetbrains.bazel.info.TargetInfo
import org.jetbrains.bazel.server.dependencygraph.DependencyGraph
import org.jetbrains.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bazel.server.sync.languages.LanguagePlugin
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.PythonBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path

class PythonLanguagePlugin(private val bazelPathsResolver: BazelPathsResolver) : LanguagePlugin<PythonModule>() {
  private var defaultInterpreter: Path? = null
  private var defaultVersion: String? = null

  override fun prepareSync(targets: Sequence<TargetInfo>, workspaceContext: WorkspaceContext) {
    val defaultTargetInfo = calculateDefaultTargetInfo(targets)
    defaultInterpreter =
      defaultTargetInfo
        ?.interpreter
        ?.takeUnless { it.relativePath.isNullOrEmpty() }
        ?.let { bazelPathsResolver.resolve(it) }
    defaultVersion = defaultTargetInfo?.version
  }

  private fun calculateDefaultTargetInfo(targets: Sequence<TargetInfo>): PythonTargetInfo? =
    targets
      .filter { it.pythonTargetInfo?.interpreter != null }
      .firstOrNull()
      ?.pythonTargetInfo

  override fun resolveModule(targetInfo: TargetInfo): PythonModule? =
    targetInfo.pythonTargetInfo?.let { pythonTargetInfo ->
      PythonModule(
        calculateInterpreterPath(interpreter = pythonTargetInfo.interpreter) ?: defaultInterpreter,
        pythonTargetInfo.version.takeUnless(String::isNullOrEmpty) ?: defaultVersion,
        pythonTargetInfo.imports,
        pythonTargetInfo.isCodeGenerator,
        generatedSources =
          pythonTargetInfo.generatedSources
            .map { bazelPathsResolver.resolve(it) },
      )
    }

  private fun calculateInterpreterPath(interpreter: FileLocation?): Path? =
    interpreter
      ?.takeUnless { it.relativePath.isNullOrEmpty() }
      ?.let { bazelPathsResolver.resolve(it) }

  override fun applyModuleData(moduleData: PythonModule, buildTarget: RawBuildTarget) {
    val interpreter = moduleData.interpreter
    buildTarget.data =
      PythonBuildTarget(
        version = moduleData.version,
        interpreter = interpreter,
        imports = moduleData.imports,
        isCodeGenerator = moduleData.isCodeGenerator,
        generatedSources = moduleData.generatedSources,
      )
  }

  override fun dependencySources(targetInfo: TargetInfo, dependencyGraph: DependencyGraph): Set<Path> =
    if (targetInfo.pythonTargetInfo != null) {
      dependencyGraph
        .transitiveDependenciesWithoutRootTargets(targetInfo.id)
        .flatMap(::getExternalSources)
        .map(::calculateExternalSourcePath)
        .toSet()
    } else {
      emptySet()
    }

  private fun getExternalSources(targetInfo: TargetInfo): List<FileLocation> =
    targetInfo.sources.mapNotNull { it.takeIf { it.isExternal } }

  private fun calculateExternalSourcePath(externalSource: FileLocation): Path {
    val path = bazelPathsResolver.resolve(externalSource)
    return bazelPathsResolver.resolve(findSitePackagesSubdirectory(path) ?: path)
  }

  private tailrec fun findSitePackagesSubdirectory(path: Path?): Path? =
    when {
      path == null -> null
      path.endsWith("site-packages") -> path
      else -> findSitePackagesSubdirectory(path.parent)
    }
}
