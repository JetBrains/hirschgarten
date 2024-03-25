package org.jetbrains.plugins.bsp.magicmetamodel.impl

import org.jetbrains.plugins.bsp.magicmetamodel.ModuleNameProvider
import org.jetbrains.plugins.bsp.magicmetamodel.ProjectDetails
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ModuleDetailsToGoModuleTransformer
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.Module
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ModuleDetailsToJavaModuleTransformer
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ModuleDetailsToPythonModuleTransformer
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ProjectDetailsToModuleDetailsTransformer
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.includesGo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.includesPython
import java.nio.file.Path

public object TargetIdToModuleEntitiesMap {
  public operator fun invoke(
      projectDetails: ProjectDetails,
      projectBasePath: Path,
      targetsMap: Map<BuildTargetId, BuildTargetInfo>,
      moduleNameProvider: ModuleNameProvider,
      hasDefaultPythonInterpreter: Boolean,
      isAndroidSupportEnabled: Boolean,
  ): Map<BuildTargetId, Module> {
    val moduleDetailsToJavaModuleTransformer = ModuleDetailsToJavaModuleTransformer(
      targetsMap,
      moduleNameProvider,
      projectBasePath,
      isAndroidSupportEnabled,
    )
    val moduleDetailsToPythonModuleTransformer = ModuleDetailsToPythonModuleTransformer(
      targetsMap,
      moduleNameProvider,
      projectBasePath,
      hasDefaultPythonInterpreter,
    )
    val moduleDetailsToGoModuleTransformer = ModuleDetailsToGoModuleTransformer(
      targetsMap,
      projectDetails,
      moduleNameProvider
    )

    val transformer = ProjectDetailsToModuleDetailsTransformer(projectDetails)

    return projectDetails.targetsId.associate {
      val moduleDetails = transformer.moduleDetailsForTargetId(it)
      val module = if (moduleDetails.target.languageIds.includesPython()) {
        moduleDetailsToPythonModuleTransformer.transform(moduleDetails)
      } else if (moduleDetails.target.languageIds.includesGo()) {
        moduleDetailsToGoModuleTransformer.transform(moduleDetails)
      } else {
        moduleDetailsToJavaModuleTransformer.transform(moduleDetails)
      }
      it.uri to module
    }
  }
}
