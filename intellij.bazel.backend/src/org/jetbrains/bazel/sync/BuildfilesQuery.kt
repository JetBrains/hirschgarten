package org.jetbrains.bazel.sync

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.bazelrunner.params.BazelFlag
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.label.AllPackagesBeneath
import org.jetbrains.bazel.label.AllRuleTargets
import org.jetbrains.bazel.label.AllRuleTargetsAndFiles
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.Main
import org.jetbrains.bazel.label.RepoType
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.toPath
import org.jetbrains.bazel.server.BazelQueryOutput
import org.jetbrains.bazel.server.BazelQueryParams
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bsp.protocol.TaskId
import java.nio.file.Path
import kotlin.collections.any
import kotlin.collections.orEmpty
import kotlin.io.path.invariantSeparatorsPathString

@ApiStatus.Internal
object BuildfilesQuery {
  suspend fun findDependantBuildFiles(
    server: BazelServerFacade,
    workspaceRelativePaths: Set<Path>,
    universeRepos: Set<RepoType>,
    repoMapping: RepoMapping,
    taskId: TaskId? = null,
  ): List<Label> {
    if (workspaceRelativePaths.isEmpty()) {
      return emptyList()
    }
    val result =
      server.query(
        BazelQueryParams(
          expression = expressionOf(workspaceRelativePaths),
          output = BazelQueryOutput.Labels,
          taskId = taskId,
          keepGoing = true,
          flags = listOf(
            BazelFlag.universeScope(universeScopeOf(universeRepos)),
            BazelFlag.orderOutput(false),
            BazelFlag.consistentLabels(true),
          ),
        ),
      )
    // an output label looks like `@@dep_repo+//lib:BUILD.bazel`, and we want `@@dep_repo+//lib:all`
    return result.result
      .filterIsInstance<ResolvedLabel>()
      .map { it.copy(target = AllRuleTargets) }
      .removeDuplicatedLocalRepos(repoMapping)
  }

  private fun expressionOf(workspaceRelativePaths: Set<Path>): String =
    "rbuildfiles(${workspaceRelativePaths.joinToString(separator = ",") { it.invariantSeparatorsPathString }})"

  private fun universeScopeOf(universeRepos: Set<RepoType>): String =
    (universeRepos + Main)
      .map { ResolvedLabel(repo = it, packagePath = AllPackagesBeneath(emptyList()), target = AllRuleTargetsAndFiles) }
      .joinToString(separator = ",")


  // remove package of the main repository that a local repository holds too,
  // local repository inside the workspace root gives every package of it two labels
  private fun List<Label>.removeDuplicatedLocalRepos(repoMapping: RepoMapping): List<Label> {
    val localPaths = (repoMapping as? BzlmodRepoMapping)?.canonicalRepoNameToLocalPath?.values.orEmpty()
    if (localPaths.isEmpty()) {
      return this
    }
    return filterNot { label ->
      val resolved = label as? ResolvedLabel ?: return@filterNot false
      resolved.repo is Main && localPaths.any { resolved.packagePath.toPath().startsWith(it) }
    }
  }
}
