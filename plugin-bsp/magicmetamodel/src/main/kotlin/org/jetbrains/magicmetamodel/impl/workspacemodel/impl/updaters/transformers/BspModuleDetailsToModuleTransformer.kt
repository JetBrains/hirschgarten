package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.JavacOptionsItem
import ch.epfl.scala.bsp4j.PythonOptionsItem
import org.jetbrains.magicmetamodel.ModuleNameProvider
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.magicmetamodel.impl.workspacemodel.GenericModuleInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.Library
import org.jetbrains.magicmetamodel.impl.workspacemodel.LibraryDependency
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDependency
import org.jetbrains.magicmetamodel.impl.workspacemodel.includesJava
import org.jetbrains.magicmetamodel.impl.workspacemodel.toModuleCapabilities

internal data class BspModuleDetails(
  val target: BuildTarget,
  val dependencySources: List<DependencySourcesItem>,
  val javacOptions: JavacOptionsItem?,
  // TODO: determine if pythonOptions has a purpose here (it's not used anywhere)
  val pythonOptions: PythonOptionsItem?,
  val type: String,
  val associates: List<BuildTargetId> = listOf(),
  val moduleDependencies: List<BuildTargetId>,
  val libraryDependencies: List<BuildTargetId>?,
)

internal class BspModuleDetailsToModuleTransformer(private val moduleNameProvider: ModuleNameProvider) :
  WorkspaceModelEntityTransformer<BspModuleDetails, GenericModuleInfo> {

  override fun transform(inputEntity: BspModuleDetails): GenericModuleInfo =
    GenericModuleInfo(
      name = moduleNameProvider(inputEntity.target.id.uri),
      type = inputEntity.type,
      modulesDependencies = inputEntity.moduleDependencies
        .map { ModuleDependency(moduleName = moduleNameProvider(it)) },
      librariesDependencies = calculateLibrariesDependencies(inputEntity),
      capabilities = inputEntity.target.capabilities.toModuleCapabilities(),
      languageIds = inputEntity.target.languageIds,
      associates = inputEntity.associates.map { it.toModuleDependency(moduleNameProvider) }
    )

  private fun calculateLibrariesDependencies(inputEntity: BspModuleDetails): List<LibraryDependency> =
    inputEntity.libraryDependencies?.map { LibraryDependency(it, true) }
      ?: if (inputEntity.target.languageIds.includesJava())
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
  private val allTargetsIds: Set<BuildTargetId>,
  private val moduleNameProvider: ModuleNameProvider,
) : WorkspaceModelEntityPartitionTransformer<BuildTarget, ModuleDependency> {

  override fun transform(inputEntity: BuildTarget): List<ModuleDependency> =
    inputEntity
      .dependencies
      .filter { allTargetsIds.contains(it.uri) }
      .map { it.uri.toModuleDependency(moduleNameProvider) }
}

internal fun BuildTargetId.toModuleDependency(moduleNameProvider: ModuleNameProvider): ModuleDependency =
  ModuleDependency(
    moduleName = moduleNameProvider(this),
  )
