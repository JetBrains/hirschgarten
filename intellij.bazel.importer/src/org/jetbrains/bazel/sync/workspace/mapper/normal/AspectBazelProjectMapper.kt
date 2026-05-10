package org.jetbrains.bazel.sync.workspace.mapper.normal

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.commons.LocalRepositoryMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.label.label
import org.jetbrains.bazel.label.toDependencyLabel
import org.jetbrains.bazel.performance.measure
import org.jetbrains.bazel.sync.workspace.BazelResolvedWorkspace
import org.jetbrains.bazel.sync.workspace.graph.DependencyGraph
import org.jetbrains.bazel.sync.workspace.languages.createLanguageProjectMappers
import org.jetbrains.bazel.sync.workspace.mapper.BazelResolvedWorkspaceBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindService
import org.jetbrains.bsp.protocol.BazelServerFacade
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.BuildTargetTag
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import kotlin.io.path.exists

internal class AspectBazelProjectMapper(
  project: Project,
  server: BazelServerFacade,
) {
  private val bazelPathsResolver = server.bazelPathsResolver
  private val langMappers = createLanguageProjectMappers(project, server)
  private val workspaceContext = server.workspaceContext

  suspend fun createProject(
    allTargets: Map<Label, TargetInfo>,
    rootTargets: Set<Label>,
    repoMapping: RepoMapping,
    hasError: Boolean,
  ): BazelResolvedWorkspace {
    val dependencyGraph =
      measure("Build dependency tree") {
        DependencyGraph(rootTargets, allTargets)
      }

    val targetsToImport: Map<Label, TargetInfo> =
      measure("Select targets") {
        dependencyGraph.allTargetsAtDepth(
          workspaceContext.importDepth,
          // Ignore .bazelbsp and all its dependencies (if any)
          predicate = { label -> label.packagePath.pathSegments.firstOrNull() != Constants.DOT_BAZELBSP_DIR_NAME }
        ).associateBy { it.label() }
      }

    langMappers.all().forEach {
      it.prepareSync(dependencyGraph, targetsToImport, repoMapping)
    }

    val rawTargets = measure("create raw targets") {
      createRawBuildTargets(targetsToImport, repoMapping, dependencyGraph)
    }

    return BazelResolvedWorkspaceBuilder.build(
      targets = rawTargets,
      hasError = hasError,
    )
  }

  private suspend fun createRawBuildTargets(
    targetsToImport: Map<Label, TargetInfo>,
    repoMapping: RepoMapping,
    dependencyGraph: DependencyGraph,
  ):List<RawBuildTarget> {
    val localRepositories = repoMapping.getLocalRepositories()
    return withContext(Dispatchers.Default) {
      val tasks =
        targetsToImport.values.map { target ->
          async {
            createRawBuildTarget(
              target,
              targetsToImport,
              repoMapping,
              dependencyGraph,
              localRepositories,
            )
          }
        }

      tasks.awaitAll()
    }
  }

  private suspend fun createRawBuildTarget(
    target: TargetInfo,
    targetsToImport: Map<Label, TargetInfo>,
    repoMapping: RepoMapping,
    dependencyGraph: DependencyGraph,
    localRepositories : LocalRepositoryMapping,
  ): RawBuildTarget {
    val label = target.label().assumeResolved()
    val targetKind = TargetKindService.getInstance().fromTargetInfo(target)
    val baseDirectory = bazelPathsResolver.toDirectoryPath(label, repoMapping)
    val localRepositories = repoMapping.getLocalRepositories()

    val buildData = ArrayList<BuildTargetData>()

    targetKind.languageClasses.map { lang ->
      langMappers.get(lang)
    }.distinct().forEach { mapper ->
      buildData.addAll(mapper.createBuildTargetData(target, targetsToImport, dependencyGraph, repoMapping))
    }

    val resources = bazelPathsResolver.resolvePaths(target.jvmTargetInfo.resourcesList, localRepositories)

    return RawBuildTarget(
      id = label,
      dependencies = target.depsList.map { it.toDependencyLabel() },
      kind = targetKind,
      sources = resolveSourceSet(target, repoMapping).toList(),
      resources = resources,
      baseDirectory = baseDirectory,
      data = buildData,
      generatorName = target.generatorName,
      isManual = BuildTargetTag.MANUAL in target.tagsList,
      isWorkspace = label.isMainWorkspace ||
                    localRepositories.localRepositories.containsKey(label.assumeResolved().repoName),
      isTestOnly = target.testonly,
    )
  }

  private fun resolveSourceSet(target: TargetInfo, repoMapping: RepoMapping): List<SourceItem> {
    val localRepositories = repoMapping.getLocalRepositories()
    return target.srcsList.mapNotNull { src ->
      val path = bazelPathsResolver.resolve(src, localRepositories)
      if (!path.exists()) {
        logger.warn("target ${target.key.label}: $path does not exist.")
        return@mapNotNull null
      }
      SourceItem(
        path = path,
        generated = !src.isSource,
      )
    }.distinctBy { it.path }
  }

  companion object {
    private val logger = logger<AspectBazelProjectMapper>()
  }
}
