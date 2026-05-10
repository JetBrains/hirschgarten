package org.jetbrains.bazel.magicmetamodel.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.ProjectDetails
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ModuleDetailsToJavaModuleTransformer
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.JvmPackagePrefixCalculator
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.JvmPackagePrefixes
import org.jetbrains.bazel.workspace.indexAdditionalFiles.ProjectViewGlobSet
import org.jetbrains.bazel.workspacemodel.entities.Module
import org.jetbrains.bsp.protocol.BuildTarget
import java.nio.file.Path

@ApiStatus.Internal
object TargetIdToModuleEntitiesMap {
  suspend operator fun invoke(
    projectDetails: ProjectDetails,
    targetIdToModuleDetails: Map<Label, ModuleDetails>,
    targetIdToTargetInfo: Map<Label, BuildTarget>,
    packagePrefixes: JvmPackagePrefixCalculator,
    fileToTargets: Map<Path, List<Label>>,
    projectBasePath: Path,
    repoMapping: RepoMapping,
    projectName: String,
    testSourcesGlob: ProjectViewGlobSet,
    testTargets: Set<Label> = emptySet(),
  ): Map<Label, List<Module>> {
    val moduleDetailsToJavaModuleTransformer =
      ModuleDetailsToJavaModuleTransformer(
        targetIdToTargetInfo,
        fileToTargets,
        projectBasePath,
        repoMapping,
        projectName,
        testSourcesGlob,
        packagePrefixes,
        testTargets = testTargets,
      )

    return withContext(Dispatchers.Default) {
      projectDetails.targetIds
        .map {
          async {
            val moduleDetails = targetIdToModuleDetails.getValue(it)
            val module =
              if (moduleDetails.target.kind.isJvmTarget()) {
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
