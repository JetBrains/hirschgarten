package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.PythonBuildTarget
import org.jetbrains.bsp.utils.extractPythonBuildTarget
import org.jetbrains.magicmetamodel.ModuleNameProvider
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.GenericModuleInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.PythonModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.PythonSdkInfo
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
