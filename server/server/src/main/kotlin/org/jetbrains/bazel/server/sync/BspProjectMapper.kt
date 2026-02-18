package org.jetbrains.bazel.server.sync

import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.jpsCompilation.utils.JPS_COMPILED_BASE_DIRECTORY
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.label.toPath
import org.jetbrains.bazel.server.bsp.info.BspInfo
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.server.model.Project
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.BspJvmClasspath
import org.jetbrains.bsp.protocol.DirectoryItem
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.InverseSourcesResult
import org.jetbrains.bsp.protocol.JvmToolchainInfo
import org.jetbrains.bsp.protocol.RawAspectTarget
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.WorkspaceBazelRepoMappingResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import java.nio.file.Path
import kotlin.io.path.isRegularFile

class BspProjectMapper(private val bazelRunner: BazelRunner, private val bspInfo: BspInfo) {
  fun workspaceTargets(project: AspectSyncProject): WorkspaceBuildTargetsResult {
    val targets =
      project.targets
        .mapValues { RawAspectTarget(it.value) }
    return WorkspaceBuildTargetsResult(
      targets = targets,
      rootTargets = project.rootTargets,
      hasError = project.hasError,
    )
  }

  suspend fun workspaceDirectories(project: Project): WorkspaceDirectoriesResult {
    // bazel symlinks exclusion logic is now taken care by BazelSymlinkExcludeService,
    // so there is no need for excluding them here anymore
    val workspaceRoot = project.workspaceRoot

    val additionalDirectoriesToExclude = computeAdditionalDirectoriesToExclude(workspaceRoot)
    val (includedDirectories, excludedDirectories) = getProjectDirs(workspaceRoot, project.workspaceContext)

    return WorkspaceDirectoriesResult(
      includedDirectories = includedDirectories.map { it.toDirectoryItem() },
      excludedDirectories = excludedDirectories.map { it.toDirectoryItem() } +
                            additionalDirectoriesToExclude.map { it.toDirectoryItem() },
    )
  }

  private data class ProjectDirs(
    val included: Set<Path>,
    val excluded: Set<Path>,
  )

  private suspend fun getProjectDirs(
    workspaceRoot: Path,
    workspaceContext: WorkspaceContext
  ): ProjectDirs {
    val included = mutableSetOf<Path>()
    val excluded = mutableSetOf<Path>()

    for (directory in workspaceContext.directories) {
      val relativeToWorkspaceRoot = workspaceRoot.resolve(directory.value).normalize()
      when (directory) {
        is ExcludableValue.Included<Path> -> included.add(relativeToWorkspaceRoot)
        is ExcludableValue.Excluded<Path> -> excluded.add(relativeToWorkspaceRoot)
      }
    }

    val includedFromTargets = mutableSetOf<Path>()
    val excludedFromTargets = mutableSetOf<Path>()
    // If we include (exclude) a directory by target, we want to add (remove) only directories of it's package
    // and not subpackages, so we need to additionally include (exclude) them.
    val excludedAdditionally = mutableSetOf<Path>()
    val includedAdditionally = mutableSetOf<Path>()

    val allSubPackages = getSubPackages(workspaceRoot, workspaceContext)
    for (target in workspaceContext.targets) {
      val path = workspaceRoot.resolve(target.value.packagePath.toPath()).normalize()
      val subPackages = allSubPackages.filter { it.startsWith(path) && it != path }.toSet()
      val (targetSet, additionalSet) = when (target) {
        is ExcludableValue.Included<Label> -> includedFromTargets to excludedAdditionally
        is ExcludableValue.Excluded<Label> -> excludedFromTargets to includedAdditionally
      }

      targetSet.add(path)
      if (target.value.isRecursive) targetSet.addAll(subPackages)
      else additionalSet.addAll(subPackages)
    }

    // We need a hierarchy of collected sets to avoid cases when one directory is both included and excluded:
    // includedAdditionally < excludedAdditionally < included < excluded < includedFromTargets < excludedFromTargets.
    // Note that at this point, if in .bazelproject file deriveTargetsFromDirectories = true, we already have all
    // items from the directories section in targets, so in practice (from the user point of view) the hierarchy is:
    // includedAdditionally < excludedAdditionally < included < includedFromTargets < excluded < excludedFromTargets.
    // When deriveTargetsFromDirectories is true we want to treat directories as if they are included as targets
    // so then, excludes always overwrite includes.
    // Otherwise, targets section items are stronger than directories section items (include form targets overwrites
    // exclude from directories).
    // This behavior is consistent with targets shown in the toolwindow.
    includedAdditionally.removePresentIn(excludedAdditionally, included, excluded, includedFromTargets, excludedFromTargets)
    excludedAdditionally.removePresentIn(included, excluded, includedFromTargets, excludedFromTargets)
    included.removePresentIn(excluded, includedFromTargets, excludedFromTargets)
    excluded.removePresentIn(includedFromTargets, excludedFromTargets)
    includedFromTargets.removePresentIn(excludedFromTargets)

    return ProjectDirs(
      included = included + includedFromTargets + includedAdditionally,
      excluded = excluded + excludedFromTargets + excludedAdditionally,
    )
  }

  private fun MutableSet<Path>.removePresentIn(vararg higherPrioritySets: Set<Path>) {
    this.removeIf { item ->
      higherPrioritySets.any { set -> item in set }
    }
  }

  private suspend fun getSubPackages(workspaceRoot: Path, workspaceContext: WorkspaceContext): Sequence<Path> {
    val patterns = workspaceContext.targets.map { it.value.assumeResolved() }.distinct()
    val out = runBazelBuildfilesQuery(workspaceContext, patterns)
    if (out.isBlank()) return emptySequence()
    return out.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { workspaceRoot.resolve(Label.parse(it).packagePath.toPath()).normalize() }
        .map { if (it.isRegularFile()) it.parent else it }
  }

  private suspend fun runBazelBuildfilesQuery(workspaceContext: WorkspaceContext, patterns: List<ResolvedLabel>): String {
    if (patterns.isEmpty()) return ""
    val expr = buildString {
      append("buildfiles(set(")
      patterns.joinTo(this, separator = " ") { it.toString() }
      append("))")
    }
    return BazelQueryRunner.runQuery(expr, bazelRunner, workspaceContext)
  }

  fun workspaceBazelRepoMapping(project: Project): WorkspaceBazelRepoMappingResult = WorkspaceBazelRepoMappingResult(project.repoMapping)

  private fun computeAdditionalDirectoriesToExclude(workspaceRoot: Path): List<Path> =
    listOf(
      bspInfo.bazelBspDir,
      workspaceRoot.resolve(JPS_COMPILED_BASE_DIRECTORY),
    )

  private fun Path.toDirectoryItem() =
    DirectoryItem(
      uri = this.toUri().toString(),
    )

  suspend fun inverseSources(project: AspectSyncProject, inverseSourcesParams: InverseSourcesParams): InverseSourcesResult {
    return InverseSourcesQuery.inverseSourcesQuery(inverseSourcesParams, project.workspaceRoot, bazelRunner, project.workspaceContext)
  }

  suspend fun jvmBuilderParamsForTarget(project: Project, target: Label): JvmToolchainInfo =
    JvmToolchainQuery.jvmToolchainQueryForTarget(bspInfo, bazelRunner, project.workspaceContext, target)

  suspend fun classpathQuery(project: Project, target: Label): BspJvmClasspath =
    ClasspathQuery.classPathQuery(target, bspInfo, bazelRunner, project.workspaceContext)

  internal object BazelQueryRunner {
    suspend fun runQuery(
      expr: String,
      bazelRunner: BazelRunner,
      workspaceContext: WorkspaceContext,
      extraOptions: List<String> = emptyList(),
    ): String {
      val command = bazelRunner.buildBazelCommand(workspaceContext, inheritProjectviewOptionsOverride = true) {
        queryExpression(expr) {
          options.add("--keep_going")
          options.addAll(extraOptions)
        }
      }
      val process = bazelRunner.runBazelCommand(command, logProcessOutput = false, taskId = null)
      val result = process.waitAndGetResult()

      if (result.isNotSuccess) {
        throw RuntimeException("bazel query failed: ${result.stderr}")
      }
      return result.stdout.decodeToString()
    }
  }
}
