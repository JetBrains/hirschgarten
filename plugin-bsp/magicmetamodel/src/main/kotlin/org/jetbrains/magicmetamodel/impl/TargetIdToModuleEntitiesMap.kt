package org.jetbrains.magicmetamodel.impl

import org.jetbrains.magicmetamodel.ModuleNameProvider
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.Module
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ModuleDetailsToGoModuleTransformer
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ModuleDetailsToJavaModuleTransformer
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ModuleDetailsToPythonModuleTransformer
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ProjectDetailsToModuleDetailsTransformer
import org.jetbrains.magicmetamodel.impl.workspacemodel.includesPython
import org.jetbrains.magicmetamodel.impl.workspacemodel.includesGo
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
      moduleNameProvider,
      projectBasePath
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
