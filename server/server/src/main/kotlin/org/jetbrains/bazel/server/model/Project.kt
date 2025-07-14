package org.jetbrains.bazel.server.model

import com.google.devtools.build.lib.query2.proto.proto2api.Build.Target
import org.jetbrains.bazel.bazelrunner.utils.BazelRelease
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.bzlmod.RepoMapping
import org.jetbrains.bazel.server.bzlmod.RepoMappingDisabled
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import java.nio.file.Path

sealed interface Project {
  val workspaceRoot: Path
  val bazelRelease: BazelRelease
  val repoMapping: RepoMapping
  val workspaceContext: WorkspaceContext
}

data class FirstPhaseProject(
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
  val modules: List<Module>,
  val libraries: Map<Label, Library>,
  val goLibraries: Map<Label, GoLibrary>,
  val nonModuleTargets: List<NonModuleTarget>, // targets that should be displayed in the project view but are neither modules nor libraries
  override val repoMapping: RepoMapping = RepoMappingDisabled,
  override val workspaceContext: WorkspaceContext,
  val workspaceName: String,
  val hasError: Boolean = false,
  val targets: Map<Label, BspTargetInfo.TargetInfo>,
) : Project {
  private val moduleMap: Map<Label, Module> = modules.associateBy(Module::label)

  fun findModule(label: Label): Module? = moduleMap[label]

  operator fun plus(project: AspectSyncProject): AspectSyncProject {
    if (workspaceRoot != project.workspaceRoot) {
      error("Cannot add projects with different workspace roots: $workspaceRoot and ${project.workspaceRoot}")
    }
    if (bazelRelease != project.bazelRelease) {
      error("Cannot add projects with different bazel versions: $bazelRelease and ${project.bazelRelease}")
    }

    val newModules = modules.toSet() + project.modules.toSet()
    val newLibraries = libraries + project.libraries
    val newGoLibraries = goLibraries + project.goLibraries
    val newNonModuleTargets = nonModuleTargets.toSet() + project.nonModuleTargets.toSet()

    return copy(
      modules = newModules.toList(),
      libraries = newLibraries,
      goLibraries = newGoLibraries,
      nonModuleTargets = newNonModuleTargets.toList(),
      workspaceContext = project.workspaceContext,
    )
  }
}
