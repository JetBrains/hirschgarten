package org.jetbrains.bazel.sync.workspace.mapper

import com.google.devtools.intellij.aspect.Common.ArtifactLocation
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import com.intellij.aspect.lib.readTargetFromFile
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.platform.util.coroutines.mapNotNullConcurrent
import com.intellij.util.containers.Interner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.NoLocalRepositories
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.label.label
import org.jetbrains.bazel.label.toDependencyLabel
import org.jetbrains.bazel.performance.measure
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.createLanguageProjectMappers
import org.jetbrains.bazel.sync.workspace.snapshot.OutputLocationCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTarget
import org.jetbrains.bazel.sync.workspace.snapshot.toWorkspaceTargetKey
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindService
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.OutputLocationCollection
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.asLogger
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension

@ApiStatus.Internal
class AspectBazelProjectMapper(
  private val project: Project,
  private val server: BazelServerFacade,
) {
  private val bazelPathsResolver = server.bazelPathsResolver
  private val langMappers = createLanguageProjectMappers()

  suspend fun mapTargets(
    targetInfoPaths: Collection<Path>,
    build: Boolean,
    taskId: TaskId,
  ): List<BuildTarget> = measure("create raw targets") {
    val taskLogger = BazelTaskEventsService.getInstance(project).asLogger(taskId)
    withContext(Dispatchers.Default) {
      targetInfoPaths
        .mapNotNullConcurrent { path ->
          val target = readTargetFromFile(path) { msg -> taskLogger.error("Could not read target info $path: $msg") }
                       ?: return@mapNotNullConcurrent null
          val isBazelBsp = target.label().packagePath.pathSegments.firstOrNull() == Constants.DOT_BAZELBSP_DIR_NAME
          if (isBazelBsp) {
            return@mapNotNullConcurrent null
          }
          createWorkspaceTarget(target = target, build = build, taskId = taskId)
        }
        .toList()
    }
  }

  private suspend fun createWorkspaceTarget(
    target: TargetIdeInfo,
    build: Boolean,
    taskId: TaskId,
  ): BuildTarget {
    val label = target.label().assumeResolved()
    val targetKind = inferTargetKind(target)

    val buildData = langMappers.all().flatMap { plugin ->
      plugin.mapBuildTargetData(server, target).also { data ->
        if (!plugin.providedBuildTargetTypes.containsAll(data.map { it::class })) {
          error("Language plugin $plugin returned unregistered build target data: ${data.joinToString()}")
        }
      }
    }

    val missingFilesReporter = MissingFilesReporter(project, taskId, label, build)

    suspend fun resolveSourceSet(
      srcs: List<ArtifactLocation>,
      category: (ArtifactLocation) -> MissingFileCategory,
    ): OutputLocationCollection {
      val existing = srcs.filter { src ->
        val path = bazelPathsResolver.resolve(src, NoLocalRepositories)
        if (!path.exists()) {
          missingFilesReporter.add(category(src), src, path)
          return@filter false
        }
        true
      }
      return OutputLocationCollectionBuilder.build(existing, server.outputParser)
    }

    return WorkspaceTarget(
      key = target.key.toWorkspaceTargetKey(),
      dependencies = target.depsList.map { it.toDependencyLabel() },
      kind = targetKind,
      sources = resolveSourceSet(target.srcsList) { src -> if (src.isSource) MissingFileCategory.SOURCES else MissingFileCategory.GENERATED_SOURCES },
      resources = resolveSourceSet(target.jvmTargetInfo.resourcesList) { MissingFileCategory.RESOURCES },
      data = buildData,
      generatorName = target.generatorName,
      isTestOnly = target.testonly,
      tags = target.tagsList.toList(),
      workspaceName = target.workspaceName,
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

      return interner.intern(TargetKind(kind = targetKind.kind, languageClasses = languages, ruleType = ruleType))
    }

    private val interner = Interner.createWeakInterner<TargetKind>()
  }
}
