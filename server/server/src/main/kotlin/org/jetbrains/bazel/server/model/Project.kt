package org.jetbrains.bazel.server.model

import com.google.devtools.build.lib.query2.proto.proto2api.Build.Target
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import java.nio.file.Path

sealed interface Project {
  val workspaceRoot: Path
  val bazelRelease: BazelRelease
  val repoMapping: RepoMapping
  val workspaceContext: WorkspaceContext
}

data class PhasedSyncProject(
  override val workspaceRoot: Path,
  override val bazelRelease: BazelRelease,
  override val repoMapping: RepoMapping,
  override val workspaceContext: WorkspaceContext,
  val modules: Map<Label, Target>,
) : Project

/** Project is the internal model of the project. Bazel/Aspect Model -> Project -> BSP Model  */
data class AspectSyncProject(
  override val workspaceRoot: Path,
  override val bazelRelease: BazelRelease,
  override val repoMapping: RepoMapping = RepoMappingDisabled,
  override val workspaceContext: WorkspaceContext,
  val workspaceName: String,
  val hasError: Boolean = false,
  val targets: Map<Label, BspTargetInfo.TargetInfo>,
  val rootTargets: Set<Label>,
) : Project {
  operator fun plus(project: AspectSyncProject): AspectSyncProject {
    if (workspaceRoot != project.workspaceRoot) {
      error("Cannot add projects with different workspace roots: $workspaceRoot and ${project.workspaceRoot}")
    }
    if (bazelRelease != project.bazelRelease) {
      error("Cannot add projects with different bazel versions: $bazelRelease and ${project.bazelRelease}")
    }

    val newTargets = targets + project.targets

    return copy(
      workspaceContext = project.workspaceContext,
      targets = newTargets,
    )
  }
}
