package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

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

private const val NO_IMPORT_PATH = "No importPath for module"

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
      importPath = goBuildInfo.importPath ?: NO_IMPORT_PATH,
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

  private fun toGoDependencies(inputEntity: ModuleDetails): List<GoModuleDependency>? {
    val result = inputEntity.moduleDependencies.mapNotNull { targetsMap[it] }.mapNotNull { buildTargetInfo ->
      val buildTarget = projectDetails.targets.find { it.id == buildTargetInfo.id.toBsp4JTargetIdentifier() }
        ?: return@mapNotNull null
      val dependencyGoBuildInfo = extractGoBuildTarget(buildTarget) ?: return@mapNotNull null
        GoModuleDependency(
          importPath = dependencyGoBuildInfo.importPath ?: NO_IMPORT_PATH,
          root = URI.create(buildTarget.baseDirectory).toPath()
        )
    }
    return result.ifEmpty { null }
  }
}
