package org.jetbrains.bazel.workspace.importer

import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProviderImpl
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.scala.sdk.ScalaSdk
import org.jetbrains.bazel.scala.sdk.scalaSdkExtension
import org.jetbrains.bazel.scala.sdk.scalaSdkExtensionExists
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterContext
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterPhase
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterResult
import org.jetbrains.bazel.sync.workspace.languages.jvm.extractScalaBuildTarget
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.OutputLocation
import java.nio.file.Path

internal class ScalaBazelWorkspaceImporter : BazelWorkspaceImporter {
  private var scalaSdks: Set<ScalaSdk>? = null

  override suspend fun import(
    context: WorkspaceImporterContext,
    phase: WorkspaceImporterPhase,
    snapshot: WorkspaceSnapshot,
  ): Result<WorkspaceImporterResult> {
    when (phase) {
      is WorkspaceImporterPhase.Initialize -> {
        if (!scalaSdkExtensionExists()) {
          return Result.success(WorkspaceImporterResult.Abort)
        }
        scalaSdks = calculateAllScalaSdkInfos(context, snapshot)
      }

      WorkspaceImporterPhase.PostProcessing -> {
        scalaSdkExtension()?.let { extension ->
          val modifiableProvider = IdeModifiableModelsProviderImpl(context.project)
          edtWriteAction {
            scalaSdks?.forEach { extension.addScalaSdk(it, modifiableProvider) }
            modifiableProvider.commit()
          }
        }
      }

      else -> {
        /* noop */
      }
    }

    return Result.success(WorkspaceImporterResult.Success)
  }

  private fun calculateAllScalaSdkInfos(context: WorkspaceImporterContext, snapshot: WorkspaceSnapshot): Set<ScalaSdk> {
    val localRepositories = snapshot.repoMapping.getLocalRepositories()
    return snapshot.targets.allTargets()
      .mapNotNull { target -> createScalaSdk(target) { context.outputResolver.resolve(it, localRepositories) } }
      .toSet()
  }

  private fun createScalaSdk(target: BuildTarget, resolveLocation: (OutputLocation) -> Path?): ScalaSdk? =
    extractScalaBuildTarget(target)
      ?.let { scalaBuildTarget ->
        ScalaSdk(
          name = scalaBuildTarget.scalaVersion.scalaVersionToScalaSdkName(),
          scalaVersion = scalaBuildTarget.scalaVersion,
          sdkJars = scalaBuildTarget.sdkJars.resolvePaths(resolveLocation).map { path -> path.toUri() },
        )
      }
}
