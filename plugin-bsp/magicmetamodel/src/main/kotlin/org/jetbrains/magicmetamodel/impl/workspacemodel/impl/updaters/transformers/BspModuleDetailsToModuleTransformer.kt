package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.Library
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.LibraryDependency
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.Module
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.ModuleDependency

internal data class BspModuleDetails(
  val target: BuildTarget,
  val allTargetsIds: List<BuildTargetIdentifier>,
  val dependencySources: List<DependencySourcesItem>,
  val type: String,
)

internal object BspModuleDetailsToModuleTransformer :
  WorkspaceModelEntityTransformer<BspModuleDetails, Module> {

  override fun transform(inputEntity: BspModuleDetails): Module {
    val buildTargetToModuleDependencyTransformer = BuildTargetToModuleDependencyTransformer(inputEntity.allTargetsIds)

    return Module(
      name = inputEntity.target.id.uri,
      type = inputEntity.type,
      modulesDependencies = buildTargetToModuleDependencyTransformer.transform(inputEntity.target),
      librariesDependencies = DependencySourcesItemToLibraryDependencyTransformer
        .transform(inputEntity.dependencySources),
    )
  }
}

internal object DependencySourcesItemToLibraryDependencyTransformer :
  WorkspaceModelEntityPartitionTransformer<DependencySourcesItem, LibraryDependency> {

  override fun transform(inputEntity: DependencySourcesItem): List<LibraryDependency> =
    DependencySourcesItemToLibraryTransformer.transform(inputEntity)
      .map(this::toLibraryDependency)

  private fun toLibraryDependency(library: Library): LibraryDependency =
    LibraryDependency(
      libraryName = library.displayName,
    )
}

internal class BuildTargetToModuleDependencyTransformer(private val allTargetsIds: List<BuildTargetIdentifier>) :
  WorkspaceModelEntityPartitionTransformer<BuildTarget, ModuleDependency> {

  override fun transform(inputEntity: BuildTarget): List<ModuleDependency> =
    inputEntity.dependencies
      .filter { allTargetsIds.contains(it) }
      .map(this::toModuleDependency)

  private fun toModuleDependency(targetId: BuildTargetIdentifier): ModuleDependency =
    ModuleDependency(
      // TODO display name?
      moduleName = targetId.uri,
    )
}
