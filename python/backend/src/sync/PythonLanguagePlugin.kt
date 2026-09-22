package com.intellij.bazel.python.backend.sync

import com.google.devtools.intellij.aspect.Common.ArtifactLocation
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.LocalRepositoryMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.python.debug.PythonDebugUtils
import org.jetbrains.bazel.python.lang.PythonBuildTarget
import org.jetbrains.bazel.python.lang.PythonLanguageClass
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.server.model.sourcesList
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.snapshot.OutputLocationCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSyncConfig
import org.jetbrains.bazel.utils.allAncestorsSequence
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.OutputLocationCollection
import org.jetbrains.bsp.protocol.mapPath
import org.jetbrains.bsp.protocol.toExecrootPath
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.name
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
    val config = PythonWorkspaceSyncConfig
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
        interpreter = pythonTarget.parseInterpreter(server),
        imports = pythonTarget.importsList.toList(),
        generatedSources = pythonTarget.resolveGeneratedSources(server, localRepositories),
        externalSources = server.outputParser.parse(getExternalSources(server, target, localRepositories))
          .map { it.toSitePackagesDirectory() }
          .let { OutputLocationCollectionBuilder.ofLocations(it) },
        mainFile = MainSourceFinder.findMainFile(target, pythonTarget, server.bazelPathsResolver, localRepositories),
        mainModule = pythonTarget.mainModule,
        runnerScript = runnerScript,
        targetArgs = PythonDebugUtils.extractPythonTargetArgs(target),
      ),
    )
  }

  private fun getExternalSources(
    server: BazelServerFacade,
    targetInfo: TargetIdeInfo,
    localRepositories: LocalRepositoryMapping,
  ): List<ArtifactLocation> =
    targetInfo.sourcesList.mapNotNull { it.takeIf { server.bazelPathsResolver.isExternal(it, localRepositories) } }.toList()

  private suspend fun IntellijIdeInfo.PythonTargetInfo.parseInterpreter(server: BazelServerFacade): OutputLocation? =
    when {
      interpreterPath.isNotEmpty() -> server.outputParser.parseExecrootPath(interpreterPath)
      hasInterpreter() && interpreter.relativePath.isNotEmpty() -> server.outputParser.parse(interpreter)
      else -> null
    }

  private suspend fun IntellijIdeInfo.PythonTargetInfo.resolveGeneratedSources(
    server: BazelServerFacade,
    localRepositories: LocalRepositoryMapping,
  ): OutputLocationCollection {
    val roots = server.outputParser.parse(generatedSourcesList)
    val files = generatedSourcesList.zip(roots)
      .flatMap { (artifact, root) ->
        val rootFile = server.bazelPathsResolver.resolve(artifact, localRepositories)
        // some code gen rules return directories. we need to figure out what files are there
        if (rootFile.isDirectory()) {
          Files.walk(rootFile).use { stream ->
            stream.toList().map { file -> file to root.mapPath { it.resolve(rootFile.relativize(file)) } }
          }
        }
        else {
          listOf(rootFile to root)
        }
      }
      .filter { (file, _) -> file.extension == "py" || file.extension == "pyw" }
      .map { (file, location) ->
        // If type annotation exists - use it instead of generated .py file
        // https://peps.python.org/pep-0484/#the-type-of-class-objects
        if (file.extension == "py" && file.resolveSibling("${file.nameWithoutExtension}.pyi").exists()) {
          location.mapPath { it.resolveSibling("${it.nameWithoutExtension}.pyi") }
        }
        else {
          location
        }
      }
    // parse again to hard link the files inside the directories and the `.pyi` stubs
    return OutputLocationCollectionBuilder.buildExecroot(files.map { it.toExecrootPath() }, server.outputParser)
  }
}

// PyNames.SITE_PACKAGES
private const val SITE_PACKAGES = "site-packages"

// the closest `site-packages` directory that contains the location, or the location itself
private fun OutputLocation.toSitePackagesDirectory(): OutputLocation = mapPath { path ->
  path.allAncestorsSequence().firstOrNull { it.name == SITE_PACKAGES } ?: path
}
