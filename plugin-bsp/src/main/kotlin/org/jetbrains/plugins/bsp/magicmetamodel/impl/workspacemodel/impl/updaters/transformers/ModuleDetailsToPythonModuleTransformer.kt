package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.PythonBuildTarget
import org.jetbrains.bsp.protocol.utils.extractPythonBuildTarget
import org.jetbrains.plugins.bsp.magicmetamodel.ModuleNameProvider
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.GenericModuleInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.PythonModule
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.PythonSdkInfo
import java.nio.file.Path

internal class ModuleDetailsToPythonModuleTransformer(
  targetsMap: Map<BuildTargetId, BuildTargetInfo>,
  moduleNameProvider: ModuleNameProvider,
  projectBasePath: Path,
  private val hasDefaultPythonInterpreter: Boolean,
) : ModuleDetailsToModuleTransformer<PythonModule>(targetsMap, moduleNameProvider) {
  override val type = "PYTHON_MODULE"

  private val sourcesItemToPythonSourceRootTransformer = SourcesItemToPythonSourceRootTransformer(projectBasePath)
  private val resourcesItemToPythonResourceRootTransformer =
    ResourcesItemToPythonResourceRootTransformer(projectBasePath)

  override fun transform(inputEntity: ModuleDetails): PythonModule =
    PythonModule(
      module = toGenericModuleInfo(inputEntity),
      sourceRoots = sourcesItemToPythonSourceRootTransformer.transform(inputEntity.sources.map {
        BuildTargetAndSourceItem(
          inputEntity.target,
          it,
        )
      }),
      resourceRoots = resourcesItemToPythonResourceRootTransformer.transform(inputEntity.resources),
      libraries = DependencySourcesItemToPythonLibraryTransformer.transform(inputEntity.dependenciesSources),
      sdkInfo = toSdkInfo(inputEntity),
    )

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

  private fun toSdkInfo(inputEntity: ModuleDetails): PythonSdkInfo? {
    val pythonBuildTarget = extractPythonBuildTarget(inputEntity.target)
    return if (pythonBuildTarget != null && pythonBuildTarget.version != null && pythonBuildTarget.interpreter != null)
      PythonSdkInfo(
        version = pythonBuildTarget.version,
        originalName = inputEntity.target.id.uri,
      )
    else
      toDefaultSdkInfo(inputEntity, pythonBuildTarget)
  }

  private fun toDefaultSdkInfo(inputEntity: ModuleDetails, pythonBuildTarget: PythonBuildTarget?): PythonSdkInfo? =
    if (hasDefaultPythonInterpreter)
      PythonSdkInfo(
        version = pythonBuildTarget?.version ?: "PY3",
        originalName = "${inputEntity.target.id.uri}-detected"
      )
    else null
}
