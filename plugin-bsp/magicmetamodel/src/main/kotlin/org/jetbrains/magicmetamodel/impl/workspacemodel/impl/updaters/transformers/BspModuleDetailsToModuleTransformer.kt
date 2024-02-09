package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.JavacOptionsItem
import ch.epfl.scala.bsp4j.PythonOptionsItem
import ch.epfl.scala.bsp4j.ScalacOptionsItem
import org.jetbrains.magicmetamodel.ModuleNameProvider
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.magicmetamodel.impl.workspacemodel.GenericModuleInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.IntermediateLibraryDependency
import org.jetbrains.magicmetamodel.impl.workspacemodel.IntermediateModuleDependency
import org.jetbrains.magicmetamodel.impl.workspacemodel.Library
import org.jetbrains.magicmetamodel.impl.workspacemodel.includesJavaOrScala
import org.jetbrains.magicmetamodel.impl.workspacemodel.toModuleCapabilities

internal data class BspModuleDetails(
  val target: BuildTarget,
  val dependencySources: List<DependencySourcesItem>,
  val javacOptions: JavacOptionsItem?,
  // TODO: determine if pythonOptions has a purpose here (it's not used anywhere)
  val pythonOptions: PythonOptionsItem?,
  val scalacOptions: ScalacOptionsItem?,
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
        .map { IntermediateModuleDependency(moduleName = moduleNameProvider(it)) },
      librariesDependencies = calculateLibrariesDependencies(inputEntity),
      capabilities = inputEntity.target.capabilities.toModuleCapabilities(),
      languageIds = inputEntity.target.languageIds,
      associates = inputEntity.associates.map { it.toModuleDependency(moduleNameProvider) },
    )

  private fun calculateLibrariesDependencies(inputEntity: BspModuleDetails): List<IntermediateLibraryDependency> =
    inputEntity.libraryDependencies?.map { IntermediateLibraryDependency(it, true) }
      ?: if (inputEntity.target.languageIds.includesJavaOrScala())
        DependencySourcesItemToLibraryDependencyTransformer
          .transform(inputEntity.dependencySources.map {
            DependencySourcesAndJvmClassPaths(it, inputEntity.toJvmClassPaths())
          }) else
        emptyList()

  private fun BspModuleDetails.toJvmClassPaths() =
    (this.javacOptions?.classpath.orEmpty() + this.scalacOptions?.classpath.orEmpty()).distinct()
}

internal object DependencySourcesItemToLibraryDependencyTransformer :
  WorkspaceModelEntityPartitionTransformer<DependencySourcesAndJvmClassPaths, IntermediateLibraryDependency> {
  override fun transform(inputEntity: DependencySourcesAndJvmClassPaths): List<IntermediateLibraryDependency> =
    DependencySourcesItemToLibraryTransformer.transform(inputEntity)
      .map { toLibraryDependency(it) }

  private fun toLibraryDependency(library: Library): IntermediateLibraryDependency =
    IntermediateLibraryDependency(
      libraryName = library.displayName,
      isProjectLevelLibrary = false,
    )
}

internal class BuildTargetToModuleDependencyTransformer(
  private val allTargetsIds: Set<BuildTargetId>,
  private val moduleNameProvider: ModuleNameProvider,
) : WorkspaceModelEntityPartitionTransformer<BuildTarget, IntermediateModuleDependency> {
  override fun transform(inputEntity: BuildTarget): List<IntermediateModuleDependency> =
    inputEntity
      .dependencies
      .filter { allTargetsIds.contains(it.uri) }
      .map { it.uri.toModuleDependency(moduleNameProvider) }
}

internal fun BuildTargetId.toModuleDependency(moduleNameProvider: ModuleNameProvider): IntermediateModuleDependency =
  IntermediateModuleDependency(
    moduleName = moduleNameProvider(this),
  )
