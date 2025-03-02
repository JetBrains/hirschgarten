package org.jetbrains.bazel.server.sync.languages.python

import org.jetbrains.bazel.info.BspTargetInfo.FileLocation
import org.jetbrains.bazel.info.BspTargetInfo.PythonTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.server.dependencygraph.DependencyGraph
import org.jetbrains.bazel.server.label.label
import org.jetbrains.bazel.server.model.Module
import org.jetbrains.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bazel.server.sync.languages.LanguagePlugin
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.PythonBuildTarget
import org.jetbrains.bsp.protocol.PythonOptionsItem
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.toPath

class PythonLanguagePlugin(private val bazelPathsResolver: BazelPathsResolver) : LanguagePlugin<PythonModule>() {
  private var defaultInterpreter: URI? = null
  private var defaultVersion: String? = null

  override fun prepareSync(targets: Sequence<TargetInfo>) {
    val defaultTargetInfo = calculateDefaultTargetInfo(targets)
    defaultInterpreter =
      defaultTargetInfo
        ?.interpreter
        ?.takeUnless { it.relativePath.isNullOrEmpty() }
        ?.let { bazelPathsResolver.resolveUri(it) }
    defaultVersion = defaultTargetInfo?.version
  }

  private fun calculateDefaultTargetInfo(targets: Sequence<TargetInfo>): PythonTargetInfo? =
    targets
      .filter(::hasPythonInterpreter)
      .firstOrNull()
      ?.pythonTargetInfo

  private fun hasPythonInterpreter(targetInfo: TargetInfo): Boolean =
    targetInfo.hasPythonTargetInfo() && targetInfo.pythonTargetInfo.hasInterpreter()

  override fun resolveModule(targetInfo: TargetInfo): PythonModule? =
    targetInfo.pythonTargetInfo?.let { pythonTargetInfo ->
      PythonModule(
        calculateInterpreterURI(interpreter = pythonTargetInfo.interpreter) ?: defaultInterpreter,
        pythonTargetInfo.version.takeUnless(String::isNullOrEmpty) ?: defaultVersion,
      )
    }

  private fun calculateInterpreterURI(interpreter: FileLocation?): URI? =
    interpreter
      ?.takeUnless { it.relativePath.isNullOrEmpty() }
      ?.let { bazelPathsResolver.resolveUri(it) }

  override fun applyModuleData(moduleData: PythonModule, buildTarget: BuildTarget) {
    val interpreter = moduleData.interpreter?.toString()
    buildTarget.data =
      PythonBuildTarget(
        version = moduleData.version,
        interpreter = interpreter,
      )
  }

  fun toPythonOptionsItem(module: Module, pythonModule: PythonModule): PythonOptionsItem =
    PythonOptionsItem(
      module.label,
      emptyList(),
    )

  override fun dependencySources(targetInfo: TargetInfo, dependencyGraph: DependencyGraph): Set<URI> =
    if (targetInfo.hasPythonTargetInfo()) {
      dependencyGraph
        .transitiveDependenciesWithoutRootTargets(targetInfo.label())
        .flatMap(::getExternalSources)
        .map(::calculateExternalSourcePath)
        .toSet()
    } else {
      emptySet()
    }

  private fun getExternalSources(targetInfo: TargetInfo): List<FileLocation> =
    targetInfo.sourcesList.mapNotNull { it.takeIf { it.isExternal } }

  private fun calculateExternalSourcePath(externalSource: FileLocation): URI {
    val path = bazelPathsResolver.resolveUri(externalSource).toPath()
    return bazelPathsResolver.resolveUri(findSitePackagesSubdirectory(path) ?: path)
  }

  private tailrec fun findSitePackagesSubdirectory(path: Path?): Path? =
    when {
      path == null -> null
      path.endsWith("site-packages") -> path
      else -> findSitePackagesSubdirectory(path.parent)
    }
}
