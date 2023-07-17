package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.JavacOptionsItem
import ch.epfl.scala.bsp4j.PythonOptionsItem
import org.jetbrains.magicmetamodel.ModuleNameProvider
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.Library
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.LibraryDependency
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.Module
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.ModuleDependency

internal data class BspModuleDetails(
  val target: BuildTarget,
  val allTargetsIds: List<BuildTargetIdentifier>,
  val dependencySources: List<DependencySourcesItem>,
  val javacOptions: JavacOptionsItem?,
  val pythonOptions: PythonOptionsItem?,
  val type: String,
  val associates: List<BuildTargetIdentifier> = listOf(),
  val moduleDependencies: List<BuildTargetIdentifier>,
  val libraryDependencies: List<BuildTargetIdentifier>?,
)

internal class BspModuleDetailsToModuleTransformer(private val moduleNameProvider: ModuleNameProvider) :
  WorkspaceModelEntityTransformer<BspModuleDetails, Module> {

  override fun transform(inputEntity: BspModuleDetails): Module =
    Module(
      name = moduleNameProvider(inputEntity.target.id),
      type = inputEntity.type,
      modulesDependencies = inputEntity.moduleDependencies
          .map { ModuleDependency(moduleName = moduleNameProvider(it)) },
      librariesDependencies = calculateLibrariesDependencies(inputEntity),
      languageIds = inputEntity.target.languageIds,
      associates = inputEntity.associates.map { it.toModuleDependency(moduleNameProvider) }
    )

  private fun calculateLibrariesDependencies(inputEntity: BspModuleDetails): List<LibraryDependency> =
    inputEntity.libraryDependencies?.map { LibraryDependency(it.uri, true) }
      ?: if (inputEntity.target.languageIds.contains("java"))
         DependencySourcesItemToLibraryDependencyTransformer
           .transform(inputEntity.dependencySources.map {
             DependencySourcesAndJavacOptions(it, inputEntity.javacOptions)
           }) else
         emptyList()
}

internal object DependencySourcesItemToLibraryDependencyTransformer :
  WorkspaceModelEntityPartitionTransformer<DependencySourcesAndJavacOptions, LibraryDependency> {

  override fun transform(inputEntity: DependencySourcesAndJavacOptions): List<LibraryDependency> =
    DependencySourcesItemToLibraryTransformer.transform(inputEntity)
      .map { toLibraryDependency(it) }

  private fun toLibraryDependency(library: Library): LibraryDependency =
    LibraryDependency(
      libraryName = library.displayName,
      isProjectLevelLibrary = false
    )
}

internal class BuildTargetToModuleDependencyTransformer(
  private val allTargetsIds: List<BuildTargetIdentifier>,
  private val moduleNameProvider: ModuleNameProvider,
) : WorkspaceModelEntityPartitionTransformer<BuildTarget, ModuleDependency> {

  override fun transform(inputEntity: BuildTarget): List<ModuleDependency> =
    inputEntity
      .dependencies
      .filter { allTargetsIds.contains(it) }
      .map { it.toModuleDependency(moduleNameProvider) }
}

internal fun BuildTargetIdentifier.toModuleDependency(moduleNameProvider: ModuleNameProvider): ModuleDependency =
  ModuleDependency(
    moduleName = moduleNameProvider(this),
  )
