package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.ProjectDetails
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.bsp.protocol.getJvmOrNull

class ProjectDetailsToModuleDetailsTransformer(private val projectDetails: ProjectDetails, private val libraryGraph: LibraryGraph) {
  private val targetsIndex = projectDetails.targets.associateBy { it.id }

  fun moduleDetailsForTargetId(targetId: Label): ModuleDetails {
    val target = targetsIndex[targetId] ?: error("Cannot find target for target id: $targetId.")
    val allDependencies = libraryGraph.calculateAllDependencies(target)
    return ModuleDetails(
      target = target,
      javacOptions = target.data?.getJvmOrNull()?.javacOpts ?: emptyList(),
      libraryDependencies = allDependencies.libraryDependencies.takeIf { projectDetails.libraries != null }?.toList(),
      moduleDependencies = allDependencies.moduleDependencies.toList(),
      defaultJdkName = projectDetails.defaultJdkName,
      jvmBinaryJars = target.data?.getJvmOrNull()?.binaryOutputs ?: emptyList(),
    )
  }
}
