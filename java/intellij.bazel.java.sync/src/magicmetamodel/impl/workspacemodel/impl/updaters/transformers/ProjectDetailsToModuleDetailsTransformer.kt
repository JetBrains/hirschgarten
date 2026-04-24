package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.ProjectDetails
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.bsp.protocol.utils.extractJvmBuildTarget

@ApiStatus.Internal
class ProjectDetailsToModuleDetailsTransformer(private val projectDetails: ProjectDetails) {
  private val targetsIndex = projectDetails.targets.associateBy { it.id }

  fun moduleDetailsForTargetId(targetId: Label): ModuleDetails {
    val target = targetsIndex[targetId] ?: error("Cannot find target for target id: $targetId.")
    val jvmBuildTarget = extractJvmBuildTarget(target)
    return ModuleDetails(
      target = target,
      javacOptions = jvmBuildTarget?.javacOpts ?: emptyList(),
      // TODO: update type and use jvm dependencies only
      dependencies = jvmBuildTarget?.jvmDependencies?.map { it.dependency } ?: target.dependencies,
      defaultJdkName = projectDetails.defaultJdkName,
      jvmBinaryJars = jvmBuildTarget?.binaryOutputs ?: emptyList(),
    )
  }
}
