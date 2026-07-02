package org.jetbrains.bazel.sync.workspace.mapper

import com.google.devtools.intellij.aspect.Common.ArtifactLocation
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.LocalRepositoryMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.label.label
import org.jetbrains.bazel.label.toDependencyLabel
import org.jetbrains.bazel.performance.measure
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.sync.workspace.graph.DependencyGraph
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.createLanguageProjectMappers
import org.jetbrains.bazel.sync.workspace.snapshot.SourceFileCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.sync.workspace.snapshot.toWorkspaceTargetKey
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindService
import org.jetbrains.bsp.protocol.BuildTargetTag
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension

@ApiStatus.Internal
class AspectBazelProjectMapper(
  project: Project,
  server: BazelServerFacade,
) {
  private val bazelPathsResolver = server.bazelPathsResolver
  private val langMappers = createLanguageProjectMappers(project, server)
  private val workspaceContext = server.workspaceContext

  suspend fun mapTargets(
    allTargets: Map<WorkspaceTargetKey, TargetIdeInfo>,
    rootTargets: Set<WorkspaceTargetKey>,
    repoMapping: RepoMapping,
  ): List<RawBuildTarget> {
    val dependencyGraph =
      measure("Build dependency tree") {
        DependencyGraph(rootTargets, allTargets)
      }

    val targetsToImport: Map<WorkspaceTargetKey, TargetIdeInfo> =
      measure("Select targets") {
        dependencyGraph.allTargetsAtDepth(
          workspaceContext.importDepth,
          // Ignore .bazelbsp and all its dependencies (if any)
          predicate = { key -> key.label.packagePath.pathSegments.firstOrNull() != Constants.DOT_BAZELBSP_DIR_NAME },
        ).associateBy { it.key.toWorkspaceTargetKey() }
      }

    langMappers.all().forEach {
      it.prepareSync(dependencyGraph, targetsToImport, repoMapping)
    }

    val rawTargets: List<RawBuildTarget> = measure("create raw targets") {
      createRawBuildTargets(targetsToImport, repoMapping, dependencyGraph)
    }

    return rawTargets
  }

  private suspend fun createRawBuildTargets(
    targetsToImport: Map<WorkspaceTargetKey, TargetIdeInfo>,
    repoMapping: RepoMapping,
    dependencyGraph: DependencyGraph,
  ): List<RawBuildTarget> {
    val localRepositories = repoMapping.getLocalRepositories()
    return withContext(Dispatchers.Default) {
      val tasks =
        targetsToImport.values.map { target ->
          async {
            createRawBuildTarget(
              target = target,
              targetsToImport = targetsToImport,
              repoMapping = repoMapping,
              dependencyGraph = dependencyGraph,
              localRepositories = localRepositories,
            )
          }
        }

      tasks.awaitAll()
    }
  }

  private suspend fun createRawBuildTarget(
    target: TargetIdeInfo,
    targetsToImport: Map<WorkspaceTargetKey, TargetIdeInfo>,
    repoMapping: RepoMapping,
    dependencyGraph: DependencyGraph,
    localRepositories: LocalRepositoryMapping,
  ): RawBuildTarget {
    val label = target.label().assumeResolved()
    val targetKind = inferTargetKind(target)
    val baseDirectory = bazelPathsResolver.toDirectoryPath(label, repoMapping)

    val buildData = langMappers.all().flatMap { mapper ->
      mapper.createBuildTargetData(target, targetsToImport, dependencyGraph, repoMapping).also { data ->
        if (!mapper.langPlugin.providedBuildTargetTypes.containsAll(data.map { it::class })) {
          error("Mapper $mapper returned unregistered build target data: ${data.joinToString()}")
        }
      }
    }

    val missingFilesReporter = MissingFilesReporter(label)

    fun resolveSourceSet(srcs: List<ArtifactLocation>, filter: (ArtifactLocation) -> Boolean = { true }): List<Path> {
      return srcs.filter(filter).mapNotNull { src: ArtifactLocation ->
        val path = bazelPathsResolver.resolve(src, localRepositories)
        if (!path.exists()) {
          missingFilesReporter.add(src, path)
          return@mapNotNull null
        }
        path
      }.distinct()
    }

    return RawBuildTarget(
      key = target.key.toWorkspaceTargetKey(),
      dependencies = target.depsList.map { it.toDependencyLabel() },
      kind = targetKind,
      sources = SourceFileCollectionBuilder.build(relativeRoot = baseDirectory, paths = resolveSourceSet(target.srcsList) { it.isSource }),
      generatedSources = SourceFileCollectionBuilder.build(
        relativeRoot = baseDirectory,
        paths = resolveSourceSet(target.srcsList) { !it.isSource },
      ),
      resources = SourceFileCollectionBuilder.build(
        relativeRoot = baseDirectory,
        paths = resolveSourceSet(target.jvmTargetInfo.resourcesList),
      ),
      baseDirectory = baseDirectory,
      data = buildData,
      generatorName = target.generatorName,
      isManual = BuildTargetTag.MANUAL in target.tagsList,
      isWorkspace = label.isMainWorkspace ||
                    localRepositories.localRepositories.containsKey(label.assumeResolved().repoName),
      isTestOnly = target.testonly,
    ).also {
      missingFilesReporter.report()
    }
  }

  private class MissingFilesReporter(val target: Label) {
    private val missingSources = ArrayList<Path>()

    fun add(src: ArtifactLocation, path: Path) {
      // Do not report missing generated sources or unknown languages/file types
      if (missingSources.size < 3 && src.isSource && LanguageClass.fromExtension(path.extension) != null)
        missingSources.add(path)
    }

    fun report() {
      if (missingSources.isNotEmpty()) {
        logger.warn(
          "target $target has ${missingSources.size} missing source files: " +
          missingSources.joinToString { it.toString() },
        )
      }
    }
  }

  companion object {
    private val logger = logger<AspectBazelProjectMapper>()

    fun inferTargetKind(target: TargetIdeInfo): TargetKind {
      val targetKind = TargetKindService.getInstance().guessFromRuleName(target.kind)

      val languages = HashSet<LanguageClass>(targetKind.languageClasses)
      LanguagePlugin.EP_NAME.forEachExtensionSafe { plugin ->
        languages.addAll(plugin.collectUsedLanguages(target))
      }

      val ruleType = when {
        !target.hasExecutableInfo() -> RuleType.LIBRARY
        targetKind.ruleType == RuleType.TEST -> RuleType.TEST
        else -> RuleType.BINARY
      }

      return TargetKind(kind = targetKind.kind, languageClasses = languages, ruleType = ruleType)
    }
  }
}
