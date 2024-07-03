package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.JavacOptionsItem
import ch.epfl.scala.bsp4j.PythonOptionsItem
import ch.epfl.scala.bsp4j.ScalacOptionsItem
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import org.jetbrains.plugins.bsp.magicmetamodel.TargetNameReformatProvider
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.GenericModuleInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.IntermediateLibraryDependency
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.IntermediateModuleDependency
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.Library
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.includesJavaOrScala
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.toBsp4JTargetIdentifier
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.toBuildTargetInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.toModuleCapabilities

internal data class BspModuleDetails(
  val target: BuildTarget,
  val dependencySources: List<DependencySourcesItem>,
  val javacOptions: JavacOptionsItem?,
  // TODO: determine if pythonOptions has a purpose here (it's not used anywhere)
  val pythonOptions: PythonOptionsItem?,
  val scalacOptions: ScalacOptionsItem?,
  val type: ModuleTypeId,
  val associates: List<BuildTargetId> = listOf(),
  val moduleDependencies: List<BuildTargetId>,
  val libraryDependencies: List<BuildTargetId>?,
)

internal class BspModuleDetailsToModuleTransformer(
  private val targetsMap: Map<BuildTargetIdentifier, BuildTargetInfo>,
  private val moduleNameProvider: TargetNameReformatProvider,
  private val libraryNameProvider: TargetNameReformatProvider,
) :
  WorkspaceModelEntityTransformer<BspModuleDetails, GenericModuleInfo> {
  override fun transform(inputEntity: BspModuleDetails): GenericModuleInfo =
    GenericModuleInfo(
      name = moduleNameProvider(inputEntity.target.toBuildTargetInfo()),
      type = inputEntity.type,
      modulesDependencies = inputEntity.moduleDependencies.mapNotNull { targetsMap[it.toBsp4JTargetIdentifier()] }
        .map { IntermediateModuleDependency(moduleName = moduleNameProvider(it)) },
      librariesDependencies = calculateLibrariesDependencies(inputEntity),
      capabilities = inputEntity.target.capabilities.toModuleCapabilities(),
      languageIds = inputEntity.target.languageIds,
      associates = inputEntity.associates.mapNotNull {
        targetsMap[it.toBsp4JTargetIdentifier()]?.toModuleDependency(moduleNameProvider)
      },
    )

  private fun calculateLibrariesDependencies(inputEntity: BspModuleDetails): List<IntermediateLibraryDependency> =
    inputEntity.libraryDependencies?.map { it.toLibraryDependency(libraryNameProvider, true) }
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
  private val allTargetsIds: Set<BuildTargetIdentifier>,
  private val targetsMap: Map<BuildTargetIdentifier, BuildTargetInfo>,
  private val moduleNameProvider: TargetNameReformatProvider,
) : WorkspaceModelEntityPartitionTransformer<BuildTarget, IntermediateModuleDependency> {
  override fun transform(inputEntity: BuildTarget): List<IntermediateModuleDependency> =
    inputEntity
      .dependencies
      .filter { allTargetsIds.contains(it) }
      .mapNotNull { targetsMap[it]?.toModuleDependency(moduleNameProvider) }
}

internal fun BuildTargetInfo.toModuleDependency(moduleNameProvider: TargetNameReformatProvider):
  IntermediateModuleDependency =
  IntermediateModuleDependency(
    moduleName = moduleNameProvider(this),
  )

internal fun BuildTargetId.toLibraryDependency(
  libraryNameProvider: TargetNameReformatProvider,
  isProjectLevelLibrary: Boolean,
): IntermediateLibraryDependency =
  IntermediateLibraryDependency(
    libraryName = libraryNameProvider(BuildTargetInfo(id = this)),
    isProjectLevelLibrary = isProjectLevelLibrary,
  )
