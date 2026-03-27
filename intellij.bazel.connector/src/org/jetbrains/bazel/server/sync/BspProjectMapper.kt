package org.jetbrains.bazel.server.sync

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.jpsCompilation.utils.JPS_COMPILED_BASE_DIRECTORY
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.label.toPath
import org.jetbrains.bazel.server.model.BazelSyncProject
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.BspJvmClasspath
import org.jetbrains.bsp.protocol.DirectoryItem
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.InverseSourcesResult
import org.jetbrains.bsp.protocol.JvmToolchainInfo
import org.jetbrains.bsp.protocol.WorkspaceBazelRepoMappingResult
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import java.nio.file.Path
import java.util.LinkedList
import kotlin.io.path.isRegularFile

@ApiStatus.Internal
class BspProjectMapper(
  private val workspaceRoot: Path,
  private val bazelRunner: BazelRunner,
  private val workspaceContext: WorkspaceContext,
) {
  suspend fun workspaceDirectories(): WorkspaceDirectoriesResult {
    // bazel symlinks exclusion logic is now taken care by BazelSymlinkExcludeService,
    // so there is no need for excluding them here anymore
    val additionalDirectoriesToExclude = computeAdditionalDirectoriesToExclude()
    val (includedDirectories, excludedDirectories) = getProjectDirs()

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

  private suspend fun getProjectDirs(): ProjectDirs {
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

    // We need a hierarchy of collected sets to avoid cases when one directory is both included and excluded.
    // The sets are prioritized according to the following order (from lowest to highest):
    // includedAdditionally < excludedAdditionally < included < excluded < includedFromTargets < excludedFromTargets.
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
      .removeShadowedPaths()

    return ProjectDirs(
      included = included + includedFromTargets + includedAdditionally,
      excluded = excluded + excludedFromTargets + excludedAdditionally,
    )
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

  fun workspaceBazelRepoMapping(project: BazelSyncProject): WorkspaceBazelRepoMappingResult = WorkspaceBazelRepoMappingResult(project.repoMapping)

  private fun computeAdditionalDirectoriesToExclude(): List<Path> =
    listOf(
      workspaceRoot.resolve(Constants.DOT_BAZELBSP_DIR_NAME),
      workspaceRoot.resolve(JPS_COMPILED_BASE_DIRECTORY),
    )

  private fun Path.toDirectoryItem() =
    DirectoryItem(
      uri = this.toUri().toString(),
    )

  internal suspend fun inverseSources(workspaceRoot: Path, inverseSourcesParams: InverseSourcesParams): InverseSourcesResult {
    return InverseSourcesQuery.inverseSourcesQuery(inverseSourcesParams, workspaceRoot, bazelRunner, workspaceContext)
  }

  suspend fun jvmBuilderParamsForTarget(target: Label): JvmToolchainInfo =
    JvmToolchainQuery.jvmToolchainQueryForTarget(bazelRunner, workspaceContext, target)

  suspend fun classpathQuery(target: Label): BspJvmClasspath =
    ClasspathQuery.classPathQuery(target, bazelRunner, workspaceContext)

  internal object BazelQueryRunner {
    suspend fun runQuery(
      expr: String,
      bazelRunner: BazelRunner,
      workspaceContext: WorkspaceContext,
      extraOptions: List<String> = emptyList(),
    ): String {
      val command = bazelRunner.buildBazelCommand(workspaceContext) {
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
