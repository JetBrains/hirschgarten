package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.openapi.project.Project
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.bazel.workspacemodel.entities.IntermediateLibraryDependency
import org.jetbrains.bazel.workspacemodel.entities.IntermediateModuleDependency
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.JavacOptionsItem
import org.jetbrains.bsp.protocol.RawBuildTarget

internal data class BspModuleDetails(
  val target: BuildTarget,
  val javacOptions: JavacOptionsItem?,
  val type: ModuleTypeId,
  val associates: List<Label> = listOf(),
  val moduleDependencies: List<Label>,
  val libraryDependencies: List<Label>?,
)

internal class BspModuleDetailsToModuleTransformer(private val targetsMap: Map<Label, BuildTarget>, private val project: Project) :
  WorkspaceModelEntityTransformer<BspModuleDetails, GenericModuleInfo> {
  override fun transform(inputEntity: BspModuleDetails): GenericModuleInfo =
    GenericModuleInfo(
      name = inputEntity.target.id.formatAsModuleName(project),
      type = inputEntity.type,
      modulesDependencies =
        inputEntity.moduleDependencies
          .mapNotNull { targetsMap[it] }
          .map { IntermediateModuleDependency(moduleName = it.id.formatAsModuleName(project)) },
      librariesDependencies = calculateLibrariesDependencies(inputEntity),
      kind = inputEntity.target.kind,
      associates =
        inputEntity.associates.mapNotNull {
          targetsMap[it]?.toModuleDependency(project)
        },
    )

  private fun calculateLibrariesDependencies(inputEntity: BspModuleDetails): List<IntermediateLibraryDependency> =
    inputEntity.libraryDependencies?.map { it.toLibraryDependency(project, true) } ?: emptyList()
}

internal class BuildTargetToModuleDependencyTransformer(
  private val allTargetsIds: Set<Label>,
  private val targetsMap: Map<Label, BuildTarget>,
  private val project: Project,
) : WorkspaceModelEntityPartitionTransformer<RawBuildTarget, IntermediateModuleDependency> {
  override fun transform(inputEntity: RawBuildTarget): List<IntermediateModuleDependency> =
    inputEntity
      .dependencies
      .filter { allTargetsIds.contains(it) }
      .mapNotNull { targetsMap[it]?.toModuleDependency(project) }
}

internal fun BuildTarget.toModuleDependency(project: Project): IntermediateModuleDependency =
  IntermediateModuleDependency(
    moduleName = this.id.formatAsModuleName(project),
  )

internal fun Label.toLibraryDependency(project: Project, isProjectLevelLibrary: Boolean): IntermediateLibraryDependency =
  IntermediateLibraryDependency(
    libraryName = this.formatAsModuleName(project),
    isProjectLevelLibrary = isProjectLevelLibrary,
  )
