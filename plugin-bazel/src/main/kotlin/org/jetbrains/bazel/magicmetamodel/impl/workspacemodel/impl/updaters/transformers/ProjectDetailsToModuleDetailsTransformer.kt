package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.magicmetamodel.ProjectDetails
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.ModuleDetails

class ProjectDetailsToModuleDetailsTransformer(private val projectDetails: ProjectDetails, private val libraryGraph: LibraryGraph) {
  private val targetsIndex = projectDetails.targets.associateBy { it.id }
  private val javacOptionsIndex = projectDetails.javacOptions.associateBy { it.target }
  private val jvmBinaryJarsIndex = projectDetails.jvmBinaryJars.groupBy { it.target }

  fun moduleDetailsForTargetId(targetId: CanonicalLabel): ModuleDetails {
    val target = targetsIndex[targetId] ?: error("Cannot find target for target id: $targetId.")
    val allDependencies = libraryGraph.calculateAllDependencies(target)
    return ModuleDetails(
      target = target,
      javacOptions = javacOptionsIndex[targetId],
      libraryDependencies = allDependencies.libraryDependencies.takeIf { projectDetails.libraries != null }?.toList(),
      moduleDependencies = allDependencies.moduleDependencies.toList(),
      defaultJdkName = projectDetails.defaultJdkName,
      jvmBinaryJars = jvmBinaryJarsIndex[targetId].orEmpty(),
    )
  }
}
