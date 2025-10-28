package org.jetbrains.bazel.magicmetamodel.impl

import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.ProjectDetails
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ModuleDetailsToJavaModuleTransformer
import org.jetbrains.bazel.workspacemodel.entities.Module
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.PartialBuildTarget
import java.nio.file.Path
import kotlin.io.path.Path

object TargetIdToModuleEntitiesMap {
  suspend operator fun invoke(
    projectDetails: ProjectDetails,
    targetIdToModuleDetails: Map<Label, ModuleDetails>,
    targetIdToTargetInfo: Map<Label, BuildTarget>,
    fileToTargetWithoutLowPrioritySharedSources: Map<Path, List<Label>>,
    projectBasePath: Path,
    project: Project,
  ): Map<Label, List<Module>> {
    val moduleDetailsToJavaModuleTransformer =
      ModuleDetailsToJavaModuleTransformer(
        targetIdToTargetInfo,
        fileToTargetWithoutLowPrioritySharedSources,
        projectBasePath,
        project,
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

@TestOnly
fun Collection<String>.toDefaultTargetsMap(): Map<Label, BuildTarget> =
  associateBy(
    keySelector = { Label.parse(it) },
    valueTransform = {
      PartialBuildTarget(
        id = Label.parse(it),
        tags = listOf(),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = emptySet(),
          ),
        baseDirectory = Path("base/dir"),
      )
    },
  )
