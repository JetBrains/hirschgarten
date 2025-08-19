package org.jetbrains.bazel.sync.workspace.languages.python

import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.info.BspTargetInfo.FileLocation
import org.jetbrains.bazel.info.BspTargetInfo.PythonTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.server.label.label
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginContext
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.PythonBuildTarget
import java.nio.file.Path

class PythonLanguagePlugin(private val bazelPathsResolver: BazelPathsResolver) : LanguagePlugin<PythonBuildTarget> {
  private var defaultInterpreter: Path? = null
  private var defaultVersion: String? = null

  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(LanguageClass.PYTHON)

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
      .filter(::hasPythonInterpreter)
      .firstOrNull()
      ?.pythonTargetInfo

  private fun hasPythonInterpreter(targetInfo: TargetInfo): Boolean =
    targetInfo.hasPythonTargetInfo() && targetInfo.pythonTargetInfo.hasInterpreter()

  override fun createBuildTargetData(context: LanguagePluginContext, target: TargetInfo): PythonBuildTarget? {
    if (!target.hasPythonTargetInfo()) {
      return null
    }
    val sourceDependencies =
      if (context.target.hasPythonTargetInfo()) {
        context.graph
          .transitiveDependenciesWithoutRootTargets(context.target.label())
          .flatMap(::getExternalSources)
          .map(::calculateExternalSourcePath)
          .distinct()
          .toList()
      } else {
        emptyList()
      }
    val pythonTarget = target.pythonTargetInfo
    return PythonBuildTarget(
      version = pythonTarget.version.takeUnless(String::isNullOrEmpty) ?: defaultVersion,
      interpreter = calculateInterpreterPath(interpreter = pythonTarget.interpreter) ?: defaultInterpreter,
      imports = pythonTarget.importsList,
      isCodeGenerator = pythonTarget.isCodeGenerator,
      generatedSources = pythonTarget.generatedSourcesList.mapNotNull { bazelPathsResolver.resolve(it) },
      sourceDependencies = sourceDependencies,
    )
  }

  private fun calculateInterpreterPath(interpreter: FileLocation?): Path? =
    interpreter
      ?.takeUnless { it.relativePath.isNullOrEmpty() }
      ?.let { bazelPathsResolver.resolve(it) }

  private fun getExternalSources(targetInfo: TargetInfo): List<FileLocation> =
    targetInfo.sourcesList.mapNotNull { it.takeIf { it.isExternal } }

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
