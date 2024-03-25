package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import org.jetbrains.bsp.protocol.utils.extractGoBuildTarget
import org.jetbrains.plugins.bsp.magicmetamodel.ModuleNameProvider
import org.jetbrains.plugins.bsp.magicmetamodel.ProjectDetails
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.GenericModuleInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.GoModule
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.GoModuleDependency
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.toBsp4JTargetIdentifier
import java.net.URI
import kotlin.io.path.toPath

internal class ModuleDetailsToGoModuleTransformer(
  private val targetsMap: Map<BuildTargetId, BuildTargetInfo>,
  private val projectDetails: ProjectDetails,
  moduleNameProvider: ModuleNameProvider,
) : ModuleDetailsToModuleTransformer<GoModule>(targetsMap, moduleNameProvider) {
  override val type = "GO_MODULE"

  override fun transform(inputEntity: ModuleDetails): GoModule {
    val goBuildInfo = extractGoBuildTarget(inputEntity.target) ?: error("Transform error, cannot extract GoBuildTarget")

    return GoModule(
      module = toGenericModuleInfo(inputEntity),
      importPath = goBuildInfo.importPath,
      root = URI.create(inputEntity.target.baseDirectory).toPath(),
      goDependencies = toGoDependencies(inputEntity)
    )
  }

  override fun toGenericModuleInfo(inputEntity: ModuleDetails): GenericModuleInfo {
    val bspModuleDetails = BspModuleDetails(
      target = inputEntity.target,
      dependencySources = inputEntity.dependenciesSources,
      type = type,
      javacOptions = null,
      pythonOptions = inputEntity.pythonOptions,
      libraryDependencies = inputEntity.libraryDependencies,
      moduleDependencies = inputEntity.moduleDependencies,
      scalacOptions = inputEntity.scalacOptions,
    )
    return bspModuleDetailsToModuleTransformer.transform(bspModuleDetails)
  }

  private fun toGoDependencies(inputEntity: ModuleDetails): List<GoModuleDependency> =
    inputEntity.moduleDependencies
      .asSequence()
      .mapNotNull { targetsMap[it] }
      .map { it.id.toBsp4JTargetIdentifier() }
      .mapNotNull { buildTargetIdentifier -> projectDetails.targets.find { it.id == buildTargetIdentifier } }
      .mapNotNull { buildTargetToGoModuleDependency(it) }
      .toList()

  private fun buildTargetToGoModuleDependency(buildTarget: BuildTarget): GoModuleDependency? =
    extractGoBuildTarget(buildTarget)?.let {
      GoModuleDependency(
        importPath = it.importPath,
        root = URI.create(buildTarget.baseDirectory).toPath()
      )
    }
}
