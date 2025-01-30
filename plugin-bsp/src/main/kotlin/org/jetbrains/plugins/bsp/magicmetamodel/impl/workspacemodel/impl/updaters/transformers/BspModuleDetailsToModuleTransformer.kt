package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.JavacOptionsItem
import ch.epfl.scala.bsp4j.ScalacOptionsItem
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import org.jetbrains.bsp.protocol.Dependency
import org.jetbrains.plugins.bsp.magicmetamodel.TargetNameReformatProvider
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo
import org.jetbrains.plugins.bsp.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.plugins.bsp.workspacemodel.entities.IntermediateLibraryDependency
import org.jetbrains.plugins.bsp.workspacemodel.entities.IntermediateModuleDependency
import org.jetbrains.plugins.bsp.workspacemodel.entities.Library
import org.jetbrains.plugins.bsp.workspacemodel.entities.includesJavaOrScala
import org.jetbrains.plugins.bsp.workspacemodel.entities.toBuildTargetInfo
import org.jetbrains.plugins.bsp.workspacemodel.entities.toModuleCapabilities

internal data class BspModuleDetails(
  val target: BuildTarget,
  val dependencySources: List<DependencySourcesItem>,
  val javacOptions: JavacOptionsItem?,
  val scalacOptions: ScalacOptionsItem?,
  val type: ModuleTypeId,
  val associates: List<BuildTargetIdentifier> = listOf(),
  val moduleDependencies: List<Dependency>,
  val libraryDependencies: List<Dependency>?,
)

internal class BspModuleDetailsToModuleTransformer(
  private val targetsMap: Map<BuildTargetIdentifier, BuildTargetInfo>,
  private val nameProvider: TargetNameReformatProvider,
) : WorkspaceModelEntityTransformer<BspModuleDetails, GenericModuleInfo> {
  override fun transform(inputEntity: BspModuleDetails): GenericModuleInfo =
    GenericModuleInfo(
      name = nameProvider(inputEntity.target.toBuildTargetInfo()),
      type = inputEntity.type,
      modulesDependencies =
        inputEntity.moduleDependencies
          .mapNotNull { dependency ->
            val target = targetsMap[dependency.id] ?: return@mapNotNull null
            IntermediateModuleDependency(moduleName = nameProvider(target), exported = dependency.exported)
          },
      librariesDependencies = calculateLibrariesDependencies(inputEntity),
      capabilities = inputEntity.target.capabilities.toModuleCapabilities(),
      languageIds = inputEntity.target.languageIds,
      associates =
        inputEntity.associates.mapNotNull { id ->
          Dependency(id, exported = false).toModuleDependency(targetsMap, nameProvider)
        },
    )

  private fun calculateLibrariesDependencies(inputEntity: BspModuleDetails): List<IntermediateLibraryDependency> =
    inputEntity.libraryDependencies?.map { it.toLibraryDependency(nameProvider, true) }
      ?: if (inputEntity.target.languageIds.includesJavaOrScala()) {
        DependencySourcesItemToLibraryDependencyTransformer
          .transform(
            inputEntity.dependencySources.map {
              DependencySourcesAndJvmClassPaths(it, inputEntity.toJvmClassPaths())
            },
          )
      } else {
        emptyList()
      }

  private fun BspModuleDetails.toJvmClassPaths() =
    (this.javacOptions?.classpath.orEmpty() + this.scalacOptions?.classpath.orEmpty()).distinct()
}

internal object DependencySourcesItemToLibraryDependencyTransformer :
  WorkspaceModelEntityPartitionTransformer<DependencySourcesAndJvmClassPaths, IntermediateLibraryDependency> {
  override fun transform(inputEntity: DependencySourcesAndJvmClassPaths): List<IntermediateLibraryDependency> =
    DependencySourcesItemToLibraryTransformer
      .transform(inputEntity)
      .map { toLibraryDependency(it) }

  private fun toLibraryDependency(library: Library): IntermediateLibraryDependency =
    IntermediateLibraryDependency(
      libraryName = library.displayName,
      isProjectLevelLibrary = false,
      exported = true,
    )
}

internal fun Dependency.toModuleDependency(
  targetsMap: Map<BuildTargetIdentifier, BuildTargetInfo>,
  moduleNameProvider: TargetNameReformatProvider,
): IntermediateModuleDependency? {
  val target = targetsMap[this.id] ?: return null
  return IntermediateModuleDependency(
    moduleName = moduleNameProvider(target),
    exported = this.exported,
  )
}

internal fun Dependency.toLibraryDependency(
  nameProvider: TargetNameReformatProvider,
  isProjectLevelLibrary: Boolean,
): IntermediateLibraryDependency =
  IntermediateLibraryDependency(
    libraryName = nameProvider(BuildTargetInfo(id = this.id)),
    isProjectLevelLibrary = isProjectLevelLibrary,
    exported = this.exported,
  )
