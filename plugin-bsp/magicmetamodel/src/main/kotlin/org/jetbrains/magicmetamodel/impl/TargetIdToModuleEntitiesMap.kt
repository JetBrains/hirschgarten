package org.jetbrains.magicmetamodel.impl

import org.jetbrains.magicmetamodel.ModuleNameProvider
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.magicmetamodel.impl.workspacemodel.Module
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ModuleDetailsToJavaModuleTransformer
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ModuleDetailsToPythonModuleTransformer
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ProjectDetailsToModuleDetailsTransformer
import org.jetbrains.magicmetamodel.impl.workspacemodel.includesPython
import java.nio.file.Path

public object TargetIdToModuleEntitiesMap {
  public operator fun invoke(
    projectDetails: ProjectDetails,
    projectBasePath: Path,
    moduleNameProvider: ModuleNameProvider,
  ): Map<BuildTargetId, Module> {
    val moduleDetailsToJavaModuleTransformer = ModuleDetailsToJavaModuleTransformer(
      moduleNameProvider,
      projectBasePath,
    )
    val moduleDetailsToPythonModuleTransformer = ModuleDetailsToPythonModuleTransformer(
      moduleNameProvider,
      projectBasePath,
    )

    val transformer = ProjectDetailsToModuleDetailsTransformer(projectDetails, projectBasePath)

    return projectDetails.targetsId.associate {
      val moduleDetails = transformer.moduleDetailsForTargetId(it)
      val module = if (moduleDetails.target.languageIds.includesPython()) {
        moduleDetailsToPythonModuleTransformer.transform(moduleDetails)
      } else {
        moduleDetailsToJavaModuleTransformer.transform(moduleDetails)
      }
      it.uri to module
    }
  }
}
