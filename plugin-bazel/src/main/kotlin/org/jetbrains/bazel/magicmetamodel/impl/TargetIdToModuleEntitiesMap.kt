package org.jetbrains.bazel.magicmetamodel.impl

import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.ProjectDetails
import org.jetbrains.bazel.magicmetamodel.TargetNameReformatProvider
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ModuleDetailsToJavaModuleTransformer
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import org.jetbrains.bazel.workspacemodel.entities.Module
import org.jetbrains.bazel.workspacemodel.entities.isJvmOrAndroidTarget
import java.nio.file.Path

object TargetIdToModuleEntitiesMap {
  suspend operator fun invoke(
    projectDetails: ProjectDetails,
    targetIdToModuleDetails: Map<Label, ModuleDetails>,
    targetIdToTargetInfo: Map<Label, BuildTargetInfo>,
    projectBasePath: Path,
    project: Project,
    nameProvider: TargetNameReformatProvider,
    isAndroidSupportEnabled: Boolean,
  ): Map<Label, Module> {
    val moduleDetailsToJavaModuleTransformer =
      ModuleDetailsToJavaModuleTransformer(
        targetIdToTargetInfo,
        nameProvider,
        projectBasePath,
        project,
        isAndroidSupportEnabled,
      )

    return withContext(Dispatchers.Default) {
      projectDetails.targetIds
        .map {
          async {
            val moduleDetails = targetIdToModuleDetails.getValue(it)
            val module =
              if (moduleDetails.target.languageIds.isJvmOrAndroidTarget()) {
                moduleDetailsToJavaModuleTransformer.transform(moduleDetails)
              } else {
                return@async null
              }
            it to module
          }
        }.awaitAll()
        .asSequence()
        .filterNotNull()
        .toMap()
    }
  }
}

@TestOnly
fun Collection<String>.toDefaultTargetsMap(): Map<Label, BuildTargetInfo> =
  associateBy(
    keySelector = { Label.parse(it) },
    valueTransform = { BuildTargetInfo(id = Label.parse(it)) },
  )
