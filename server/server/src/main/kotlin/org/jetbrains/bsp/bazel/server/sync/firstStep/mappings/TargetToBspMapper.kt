package org.jetbrains.bsp.bazel.server.sync.firstStep.mappings

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.ResourcesParams
import ch.epfl.scala.bsp4j.ResourcesResult
import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.SourcesResult
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import com.google.devtools.build.lib.query2.proto.proto2api.Build.Target
import org.jetbrains.bsp.bazel.server.model.BspMappings
import org.jetbrains.bsp.bazel.server.model.Project
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContextProvider
import java.nio.file.Path

class TargetToBspMapper(private val workspaceContextProvider: WorkspaceContextProvider, private val workspaceRoot: Path) {
  fun toWorkspaceBuildTargetsResult(project: Project): WorkspaceBuildTargetsResult {
    val shouldSyncManualTargets = workspaceContextProvider.currentWorkspaceContext().allowManualTargetsSync.value

    val targets =
      project.lightweightModules
        ?.values
        .orEmpty()
        .filter { it.isSupported() }
        .filter { shouldSyncManualTargets || !it.isManual() }
        .filterNot { it.isNoIde() }
        .map { it.toBspBuildTarget() }

    return WorkspaceBuildTargetsResult(targets)
  }

  fun toSourcesResult(project: Project, sourcesParams: SourcesParams): SourcesResult {
    val items =
      project
        .lightweightModulesForTargets(sourcesParams.targets)
        .map { it.toBspSourcesItem(workspaceRoot) }

    return SourcesResult(items)
  }

  fun toResourcesResult(project: Project, resourcesParams: ResourcesParams): ResourcesResult {
    val items =
      project
        .lightweightModulesForTargets(resourcesParams.targets)
        .map { it.toBspSourcesItem(workspaceRoot) }

    return ResourcesResult(emptyList())
  }

  private fun Project.lightweightModulesForTargets(targets: List<BuildTargetIdentifier>): List<Target> =
    BspMappings.toLabels(targets).mapNotNull { lightweightModules?.get(it) }
}
