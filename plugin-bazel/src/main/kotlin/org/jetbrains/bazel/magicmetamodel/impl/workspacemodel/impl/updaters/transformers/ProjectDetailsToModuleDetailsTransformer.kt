package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import org.jetbrains.bazel.magicmetamodel.ProjectDetails
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.bsp.protocol.BuildTargetIdentifier

private const val WORKSPACE_MODEL_ENTITIES_FOLDER_MARKER = "workspace-model-entities-folder-marker"

class ProjectDetailsToModuleDetailsTransformer(private val projectDetails: ProjectDetails, private val libraryGraph: LibraryGraph) {
  private val targetsIndex = projectDetails.targets.associateBy { it.id }
  private val sourcesIndex = projectDetails.sources.groupBy { it.target }
  private val resourcesIndex = projectDetails.resources.groupBy { it.target }
  private val jvmBinaryJarsIndex = projectDetails.jvmBinaryJars.groupBy { it.target }

  fun moduleDetailsForTargetId(targetId: BuildTargetIdentifier): ModuleDetails {
    val target = targetsIndex[targetId] ?: error("Cannot find target for target id: $targetId.")
    val allDependencies = libraryGraph.calculateAllDependencies(target)
    return ModuleDetails(
      target = target,
      sources = sourcesIndex[target.id].orEmpty(),
      resources = resourcesIndex[targetId].orEmpty(),
      outputPathUris = emptyList(),
      libraryDependencies = allDependencies.libraryDependencies.toList(),
      moduleDependencies = allDependencies.moduleDependencies.toList(),
      defaultJdkName = projectDetails.defaultJdkName,
      jvmBinaryJars = jvmBinaryJarsIndex[targetId].orEmpty(),
      workspaceModelEntitiesFolderMarker =
        resourcesIndex[targetId].orEmpty().flatMap { it.resources }.any { it.endsWith(WORKSPACE_MODEL_ENTITIES_FOLDER_MARKER) },
    )
  }
}
