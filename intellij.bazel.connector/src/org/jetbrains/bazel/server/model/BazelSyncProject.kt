package org.jetbrains.bazel.server.model

import com.google.devtools.intellij.aspect.Common.ArtifactLocation
import com.google.devtools.build.lib.query2.proto.proto2api.Build.Target
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.label.Label
import java.nio.file.Path

@ApiStatus.Internal
data class PhasedSyncProject(
  val workspaceRoot: Path,
  val bazelRelease: BazelRelease,
  val repoMapping: RepoMapping,
  val modules: Map<Label, Target>,
  val hasError: Boolean = false,
)

/** Project is the internal model of the project. Bazel/Aspect Model -> Project -> BSP Model  */
@ApiStatus.Internal
data class AspectSyncProject(
  val workspaceRoot: Path,
  val bazelRelease: BazelRelease,
  val repoMapping: RepoMapping = RepoMappingDisabled,
  val workspaceName: String,
  val hasError: Boolean = false,
  val targets: Map<Label, IntellijIdeInfo.TargetIdeInfo>,
  val rootTargets: Set<Label>,
) {
  operator fun plus(project: AspectSyncProject): AspectSyncProject {
    if (workspaceRoot != project.workspaceRoot) {
      error("Cannot add projects with different workspace roots: $workspaceRoot and ${project.workspaceRoot}")
    }
    if (bazelRelease != project.bazelRelease) {
      error("Cannot add projects with different bazel versions: $bazelRelease and ${project.bazelRelease}")
    }

    val newTargets = targets + project.targets
    return copy(targets = newTargets)
  }
}

@get:ApiStatus.Internal
val IntellijIdeInfo.TargetIdeInfo.sourcesList: Sequence<ArtifactLocation>
  get() = srcsList.asSequence().filter { it.isSource }

@get:ApiStatus.Internal
val IntellijIdeInfo.TargetIdeInfo.generatedSourcesList: Sequence<ArtifactLocation>
  get() = srcsList.asSequence().filter { !it.isSource }
