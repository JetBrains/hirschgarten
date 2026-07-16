package org.jetbrains.bazel.sync.workspace.mapper

import com.google.devtools.intellij.aspect.Common.ArtifactLocation
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
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
import org.jetbrains.bazel.languages.projectview.importDepth
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
import org.jetbrains.bsp.protocol.TaskId
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension

@ApiStatus.Internal
class AspectBazelProjectMapper(
  private val project: Project,
  server: BazelServerFacade,
) {
  private val bazelPathsResolver = server.bazelPathsResolver
  private val langMappers = createLanguageProjectMappers(project, server)
  private val projectView = server.projectView

  suspend fun mapTargets(
    allTargets: Map<WorkspaceTargetKey, TargetIdeInfo>,
    rootTargets: Set<WorkspaceTargetKey>,
    repoMapping: RepoMapping,
    build: Boolean,
    taskId: TaskId,
  ): List<RawBuildTarget> {
    val dependencyGraph =
      measure("Build dependency tree") {
        DependencyGraph(rootTargets, allTargets)
      }

    val targetsToImport: Map<WorkspaceTargetKey, TargetIdeInfo> =
      measure("Select targets") {
        dependencyGraph.allTargetsAtDepth(
          projectView.importDepth,
          // Ignore .bazelbsp and all its dependencies (if any)
          predicate = { key -> key.label.packagePath.pathSegments.firstOrNull() != Constants.DOT_BAZELBSP_DIR_NAME },
        ).associateBy { it.key.toWorkspaceTargetKey() }
      }

    langMappers.all().forEach {
      it.prepareSync(dependencyGraph, targetsToImport, repoMapping)
    }

    val rawTargets: List<RawBuildTarget> = measure("create raw targets") {
      createRawBuildTargets(targetsToImport, repoMapping, dependencyGraph, build, taskId)
    }

    return rawTargets
  }

  private suspend fun createRawBuildTargets(
    targetsToImport: Map<WorkspaceTargetKey, TargetIdeInfo>,
    repoMapping: RepoMapping,
    dependencyGraph: DependencyGraph,
    build: Boolean,
    taskId: TaskId,
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
              build = build,
              taskId = taskId,
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
    build: Boolean,
    taskId: TaskId,
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

    val missingFilesReporter = MissingFilesReporter(project, taskId, label, build)

    fun resolveSourceSet(srcs: List<ArtifactLocation>, category: MissingFileCategory): List<Path> {
      return srcs.mapNotNull { src: ArtifactLocation ->
        val path = bazelPathsResolver.resolve(src, localRepositories)
        if (!path.exists()) {
          missingFilesReporter.add(category, src, path)
          return@mapNotNull null
        }
        path
      }.distinct()
    }

    return RawBuildTarget(
      key = target.key.toWorkspaceTargetKey(),
      dependencies = target.depsList.map { it.toDependencyLabel() },
      kind = targetKind,
      sources = SourceFileCollectionBuilder.build(
        relativeRoot = baseDirectory,
        paths = resolveSourceSet(target.srcsList.filter { it.isSource }, MissingFileCategory.SOURCES),
      ),
      generatedSources = SourceFileCollectionBuilder.build(
        relativeRoot = baseDirectory,
        paths = resolveSourceSet(target.srcsList.filter { !it.isSource }, MissingFileCategory.GENERATED_SOURCES),
      ),
      resources = SourceFileCollectionBuilder.build(
        relativeRoot = baseDirectory,
        paths = resolveSourceSet(target.jvmTargetInfo.resourcesList, MissingFileCategory.RESOURCES),
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

  private enum class MissingFileCategory(val title: String, val condition: (file: MissingFile, build: Boolean) -> Boolean) {
    SOURCES(
      "Source files",
      { file, build ->
        LanguageClass.fromExtension(file.path.extension) != null
      },
    ),
    GENERATED_SOURCES(
      "Generated sources (files that were not materialized during sync, so they cannot be indexed by the IDE)",
      { file, build ->
        // report missing generated source only if build was requested
        build && LanguageClass.fromExtension(file.path.extension) != null
      },
    ),
    RESOURCES(
      "Resources",
      { file, build ->
        file.src.isSource || build
      },
    ),
  }

  private class MissingFilesReporter(
    private val project: Project,
    private val taskId: TaskId,
    private val target: Label,
    private val build: Boolean,
  ) {
    private val stored = MissingFileCategory.entries.associateWith { ArrayList<MissingFile>() }
    private val totals = IntArray(MissingFileCategory.entries.size)

    fun add(category: MissingFileCategory, src: ArtifactLocation, path: Path) {
      val file = MissingFile(src, path)
      if (!category.condition(file, build))
        return

      totals[category.ordinal]++
      val list = stored.getValue(category)
      if (list.size < DISPLAY_CAP) list.add(file)
    }

    fun report() {
      val grandTotal = totals.sum()
      if (grandTotal == 0)
        return

      val description = buildString {
        for (category in MissingFileCategory.entries) {
          val total = totals[category.ordinal]
          if (total == 0)
            continue

          this.appendLine(category.title)
          val files = stored.getValue(category)
          files.forEach { file ->
            append("  ")
              .append(file.src.rootPath).append("/").append(file.src.relativePath)
              .append(" (").append(file.path.toString()).append(")")
              .append('\n')
          }
          if (total > files.size)
            append("  ... and ").append(total - files.size).append(" more\n")
        }
      }

      log.error("Target ${target} has missing $grandTotal ${StringUtil.pluralize("file", grandTotal)}: " + description)
    }

    companion object {
      private const val DISPLAY_CAP = 16
    }
  }

  private data class MissingFile(val src: ArtifactLocation, val path: Path)

  companion object {
    private val log = logger<AspectBazelProjectMapper>()

    fun inferTargetKind(target: TargetIdeInfo): TargetKind {
      val targetKind = TargetKindService.getInstance().guessFromRuleName(target.kind)

      val languages = HashSet<LanguageClass>(targetKind.languageClasses)
      LanguagePlugin.EP_NAME.forEachExtensionSafe { plugin ->
        languages.addAll(plugin.collectUsedLanguages(target))
      }

      val ruleType = when {
        targetKind.ruleType == RuleType.BINARY -> RuleType.BINARY
        targetKind.ruleType == RuleType.TEST -> RuleType.TEST
        !target.hasExecutableInfo() -> RuleType.LIBRARY
        else -> RuleType.BINARY
      }

      return TargetKind(kind = targetKind.kind, languageClasses = languages, ruleType = ruleType)
    }
  }
}
