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
import org.jetbrains.bazel.workspacemodel.entities.isJvmOrAndroidTarget
import org.jetbrains.bsp.protocol.BuildTarget
import java.nio.file.Path
import kotlin.io.path.Path

object TargetIdToModuleEntitiesMap {
  suspend operator fun invoke(
    projectDetails: ProjectDetails,
    targetIdToModuleDetails: Map<Label, ModuleDetails>,
    targetIdToTargetInfo: Map<Label, BuildTarget>,
    fileToTarget: Map<Path, List<Label>>,
    projectBasePath: Path,
    project: Project,
    isAndroidSupportEnabled: Boolean,
  ): Map<Label, List<Module>> {
    val moduleDetailsToJavaModuleTransformer =
      ModuleDetailsToJavaModuleTransformer(
        targetIdToTargetInfo,
        fileToTarget,
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
fun Collection<String>.toDefaultTargetsMap(): Map<Label, BuildTarget> =
  associateBy(
    keySelector = { Label.parse(it) },
    valueTransform = {
      BuildTarget(
        id = Label.parse(it),
        tags = listOf(),
        languageIds = listOf(),
        dependencies = listOf(),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
          ),
        sources = listOf(),
        resources = listOf(),
        baseDirectory = Path("base/dir"),
      )
    },
  )
