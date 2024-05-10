package org.jetbrains.plugins.bsp.magicmetamodel.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.jetbrains.plugins.bsp.magicmetamodel.ProjectDetails
import org.jetbrains.plugins.bsp.magicmetamodel.TargetNameReformatProvider
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.Module
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ModuleDetailsToJavaModuleTransformer
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ModuleDetailsToPythonModuleTransformer
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ProjectDetailsToModuleDetailsTransformer
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.includesPython
import java.nio.file.Path

public object TargetIdToModuleEntitiesMap {
  public operator fun invoke(
    projectDetails: ProjectDetails,
    projectBasePath: Path,
    targetsMap: Map<BuildTargetId, BuildTargetInfo>,
    moduleNameProvider: TargetNameReformatProvider,
    libraryNameProvider: TargetNameReformatProvider,
    hasDefaultPythonInterpreter: Boolean,
    isAndroidSupportEnabled: Boolean,
  ): Map<BuildTargetId, Module> {
    val moduleDetailsToJavaModuleTransformer = ModuleDetailsToJavaModuleTransformer(
      targetsMap,
      moduleNameProvider,
      libraryNameProvider,
      projectBasePath,
      isAndroidSupportEnabled,
    )
    val moduleDetailsToPythonModuleTransformer = ModuleDetailsToPythonModuleTransformer(
      targetsMap,
      moduleNameProvider,
      libraryNameProvider,
      hasDefaultPythonInterpreter,
    )

    val transformer = ProjectDetailsToModuleDetailsTransformer(projectDetails)

    return runBlocking(Dispatchers.Default) {
      projectDetails.targetsId.map {
        async {
          val moduleDetails = transformer.moduleDetailsForTargetId(it)
          val module = if (moduleDetails.target.languageIds.includesPython()) {
            moduleDetailsToPythonModuleTransformer.transform(moduleDetails)
          } else {
            moduleDetailsToJavaModuleTransformer.transform(moduleDetails)
          }
          it.uri to module
        }
      }.awaitAll().toMap()
    }
  }
}
