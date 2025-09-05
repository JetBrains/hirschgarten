package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.ProjectDetails
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.bsp.protocol.utils.extractJvmBuildTarget

class ProjectDetailsToModuleDetailsTransformer(private val projectDetails: ProjectDetails, private val libraryGraph: LibraryGraph) {
  private val targetsIndex = projectDetails.targets.associateBy { it.id }

  fun moduleDetailsForTargetId(targetId: Label): ModuleDetails {
    val target = targetsIndex[targetId] ?: error("Cannot find target for target id: $targetId.")
    return ModuleDetails(
      target = target,
      javacOptions = extractJvmBuildTarget(target)?.javacOpts ?: emptyList(),
      dependencies = libraryGraph.calculateAllDependencies(target),
      defaultJdkName = projectDetails.defaultJdkName,
      jvmBinaryJars = extractJvmBuildTarget(target)?.binaryOutputs ?: emptyList(),
    )
  }
}
