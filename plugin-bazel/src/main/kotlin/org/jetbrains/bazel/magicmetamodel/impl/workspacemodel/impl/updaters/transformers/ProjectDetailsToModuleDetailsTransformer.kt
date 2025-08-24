package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.ProjectDetails
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.ModuleDetails

class ProjectDetailsToModuleDetailsTransformer(private val projectDetails: ProjectDetails, private val libraryGraph: LibraryGraph) {
  private val targetsIndex = projectDetails.targets.associateBy { it.id }
  private val javacOptionsIndex = projectDetails.javacOptions.associateBy { it.target }
  private val jvmBinaryJarsIndex = projectDetails.jvmBinaryJars.groupBy { it.target }

  fun moduleDetailsForTargetId(targetId: Label): ModuleDetails {
    val target = targetsIndex[targetId] ?: error("Cannot find target for target id: $targetId.")
    return ModuleDetails(
      target = target,
      javacOptions = javacOptionsIndex[targetId],
      dependencies = libraryGraph.calculateAllDependencies(target),
      defaultJdkName = projectDetails.defaultJdkName,
      jvmBinaryJars = jvmBinaryJarsIndex[targetId].orEmpty(),
    )
  }
}
