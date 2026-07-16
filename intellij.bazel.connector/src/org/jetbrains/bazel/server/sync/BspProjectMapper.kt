package org.jetbrains.bazel.server.sync

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.directories
import org.jetbrains.bazel.languages.projectview.targets
import org.jetbrains.bsp.protocol.DirectoryItem
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.InverseSourcesResult
import org.jetbrains.bsp.protocol.JvmToolchainInfo
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import java.nio.file.Path
import java.util.LinkedList
import kotlin.io.path.isRegularFile

@ApiStatus.Internal
class BspProjectMapper(
  private val workspaceRoot: Path,
  private val bazelRunner: BazelRunner,
  private val projectView: ProjectView,
  private val bazelPathsResolver: BazelPathsResolver,
) {
  suspend fun workspaceDirectories(repoMapping: RepoMapping, taskId: TaskId): WorkspaceDirectoriesResult {
    val (includedDirectories, excludedDirectories) = getProjectDirs(repoMapping, taskId)
    return WorkspaceDirectoriesResult(
      includedDirectories = includedDirectories.map { it.toDirectoryItem() },
      excludedDirectories = excludedDirectories.map { it.toDirectoryItem() },
    )
  }

  private data class ProjectDirs(
    val included: Set<Path>,
    val excluded: Set<Path>,
  )

  private suspend fun getProjectDirs(repoMapping: RepoMapping, taskId: TaskId): ProjectDirs {
    val excludedTechnicalDirectories = getTechnicalDirectoriesToExclude()
    val included = mutableSetOf<Path>()
    val excluded = mutableSetOf<Path>()

    val directories = projectView.directories
    for (directory in directories) {
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

    val allSubPackages = getSubPackages(projectView, repoMapping, taskId)
    for (target in projectView.targets) {
      val path = target.value.assumeResolved().toDirectoryPath(repoMapping)
      val subPackages = allSubPackages.filter { it.startsWith(path) && it != path }
      val (targetSet, additionalSet) = when (target) {
        is ExcludableValue.Included<Label> -> includedFromTargets to excludedAdditionally
        is ExcludableValue.Excluded<Label> -> excludedFromTargets to includedAdditionally
      }

      targetSet.add(path)
      if (target.value.isRecursive) targetSet.addAll(subPackages)
      else additionalSet.addAll(subPackages)
    }

    // We need a hierarchy of collected sets to avoid cases when one directory is both included and excluded.
    // The sets are prioritized according to the following order (from lowest to highest):
    // includedAdditionally < excludedAdditionally < included < excluded < includedFromTargets < excludedFromTargets < excludedTechnicalDirectories.
    //
    // Note that 'included' and 'excluded' sets (derived from the 'directories' section of the .bazelproject file)
    // are matched resursively, meaning that if a directory is included, all its subdirectories are also
    // included (unless overwritten by a higher priority set).
    // All other sets (derived from the 'targets' section) are matched exactly.
    //
    // This hierarchy ensures that:
    // 1. Targets section items are stronger than directories section items (e.g., including a target
    //    overwrites an exclusion of its parent directory).
    // 2. Excludes within the same section (directories or targets) overwrite includes.
    // 3. Subpackages of non-recursive targets (included/excluded additionally) have the lowest priority,
    //    ensuring they are only used as a fallback if no other rule applies.
    //
    // Note that if 'derive_targets_from_directories' is true, directories are also present in the targets
    // sets, which effectively moves them to a higher priority position.
    DirsPriority()
      .addLast(includedAdditionally)
      .addLast(excludedAdditionally)
      .addLast(included, isRecursive = true)
      .addLast(excluded, isRecursive = true)
      .addLast(includedFromTargets)
      .addLast(excludedFromTargets)
      .addLast(excludedTechnicalDirectories, isRecursive = true)
      .removeShadowedPaths()

    return ProjectDirs(
      included = included + includedFromTargets + includedAdditionally,
      excluded = excluded + excludedFromTargets + excludedAdditionally + excludedTechnicalDirectories,
    )
  }

  private suspend fun getSubPackages(projectView: ProjectView, repoMapping: RepoMapping, taskId: TaskId): List<Path> {
    val patterns = projectView.targets.map { it.value.assumeResolved() }.distinct()
    val out = runBazelBuildfilesQuery(projectView, patterns, taskId)
    if (out.isBlank())
      return emptyList()

    val result = ArrayList<Path>()
    for (line in out.lines()) {
      if (line.isBlank())
        continue

      val labelPath = Label.parseOrNull(line.trim())?.assumeResolved()?.toDirectoryPath(repoMapping)
                      ?: continue
      val dirPath = if (labelPath.isRegularFile()) labelPath.parent else labelPath
      result.add(dirPath)
    }
    return result
  }

  private fun ResolvedLabel.toDirectoryPath(repoMapping: RepoMapping): Path =
    bazelPathsResolver.toDirectoryPath(this, repoMapping).normalize()

  private suspend fun runBazelBuildfilesQuery(projectView: ProjectView, patterns: List<ResolvedLabel>, taskId: TaskId): String {
    if (patterns.isEmpty()) return ""
    val expr = buildString {
      append("buildfiles(set(")
      patterns.joinTo(this, separator = " ") { it.toString() }
      append("))")
    }

    val command = bazelRunner.buildBazelCommand(projectView) {
      queryExpression(expr) {
        options.add("--keep_going")
      }
    }
    val process = bazelRunner.runBazelCommand(command, logProcessOutput = false, taskId = taskId)
    val result = process.waitAndGetResult()

    if (result.isNotSuccess) {
      throw RuntimeException("bazel query failed: ${result.stderrLines.joinToString("\n")}")
    }
    return result.stdout.decodeToString()
  }

  // bazel symlinks exclusion logic is taken care by BazelSymlinkExcludeService
  private fun getTechnicalDirectoriesToExclude(): MutableSet<Path> =
    mutableSetOf(
      workspaceRoot.resolve(Constants.DOT_BAZELBSP_DIR_NAME),
    )

  private fun Path.toDirectoryItem() =
    DirectoryItem(
      uri = this.toUri().toString(),
    )

  internal suspend fun inverseSources(workspaceRoot: Path, inverseSourcesParams: InverseSourcesParams): InverseSourcesResult {
    return InverseSourcesQuery.inverseSourcesQuery(inverseSourcesParams, workspaceRoot, bazelRunner, projectView)
  }

  suspend fun jvmBuilderParamsForTarget(target: Label): JvmToolchainInfo =
    JvmToolchainQuery.jvmToolchainQueryForTarget(bazelRunner, projectView, target)


  private class DirsPriority {
    private val prioritizedPathSets: LinkedList<PathSet> = LinkedList()

    fun addLast(paths: MutableSet<Path>, isRecursive: Boolean = false): DirsPriority {
      prioritizedPathSets.addLast(PathSet(paths, isRecursive))
      return this
    }

    fun removeShadowedPaths() {
      for (i in prioritizedPathSets.indices) {
        val current = prioritizedPathSets[i]
        val higherPrioritySets = prioritizedPathSets.subList(i + 1, prioritizedPathSets.size)
        current.paths.removeShadowedBy(higherPrioritySets)
      }
    }

    private fun MutableSet<Path>.removeShadowedBy(higherPrioritySets: List<PathSet>) =
      this.removeIf { path ->
        higherPrioritySets.any { higherPrioritySet -> higherPrioritySet.containsPath(path) }
      }

    private fun PathSet.containsPath(otherPath: Path): Boolean =
      if (isRecursive) {
        paths.any { otherPath.startsWith(it) }
      }
      else {
        otherPath in paths
      }

    private class PathSet(
      val paths: MutableSet<Path>,
      val isRecursive: Boolean,
    )
  }
}
