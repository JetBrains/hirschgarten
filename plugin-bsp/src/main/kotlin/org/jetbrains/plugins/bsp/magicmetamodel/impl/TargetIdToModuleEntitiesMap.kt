package org.jetbrains.plugins.bsp.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.TestOnly
import org.jetbrains.plugins.bsp.magicmetamodel.ProjectDetails
import org.jetbrains.plugins.bsp.magicmetamodel.TargetNameReformatProvider
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.Module
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ModuleDetailsToJavaModuleTransformer
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ModuleDetailsToPythonModuleTransformer
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.includesPython

internal object TargetIdToModuleEntitiesMap {
  operator fun invoke(
      projectDetails: ProjectDetails,
      targetIdToModuleDetails: Map<BuildTargetIdentifier, ModuleDetails>,
      targetIdToTargetInfo: Map<BuildTargetIdentifier, BuildTargetInfo>,
      projectBasePath: Path,
      moduleNameProvider: TargetNameReformatProvider,
      libraryNameProvider: TargetNameReformatProvider,
      hasDefaultPythonInterpreter: Boolean,
      isAndroidSupportEnabled: Boolean,
  ): Map<BuildTargetIdentifier, Module> {
    val moduleDetailsToJavaModuleTransformer =
        ModuleDetailsToJavaModuleTransformer(
            targetIdToTargetInfo,
            moduleNameProvider,
            libraryNameProvider,
            projectBasePath,
            isAndroidSupportEnabled,
        )
    val moduleDetailsToPythonModuleTransformer =
        ModuleDetailsToPythonModuleTransformer(
            targetIdToTargetInfo,
            moduleNameProvider,
            libraryNameProvider,
            hasDefaultPythonInterpreter,
        )

    return runBlocking(Dispatchers.Default) {
      projectDetails.targetIds
          .map {
            async {
              val moduleDetails = targetIdToModuleDetails.getValue(it)
              val module =
                  if (moduleDetails.target.languageIds.includesPython()) {
                    moduleDetailsToPythonModuleTransformer.transform(moduleDetails)
                  } else {
                    moduleDetailsToJavaModuleTransformer.transform(moduleDetails)
                  }
              it to module
            }
          }
          .awaitAll()
          .toMap()
    }
  }
}

@TestOnly
public fun Collection<String>.toDefaultTargetsMap(): Map<BuildTargetIdentifier, BuildTargetInfo> =
    associateBy(
        keySelector = { BuildTargetIdentifier(it) },
        valueTransform = { BuildTargetInfo(id = BuildTargetIdentifier(it)) })
