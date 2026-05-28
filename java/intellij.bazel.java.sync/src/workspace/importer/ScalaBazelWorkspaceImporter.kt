package org.jetbrains.bazel.workspace.importer

import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProviderImpl
import org.jetbrains.bazel.scala.sdk.ScalaSdk
import org.jetbrains.bazel.scala.sdk.scalaSdkExtension
import org.jetbrains.bazel.scala.sdk.scalaSdkExtensionExists
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterContext
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterPhase
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterResult
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.utils.extractScalaBuildTarget

internal class ScalaBazelWorkspaceImporter : BazelWorkspaceImporter {
  private var scalaSdks: Set<ScalaSdk>? = null

  override suspend fun import(
    context: WorkspaceImporterContext,
    phase: WorkspaceImporterPhase,
    snapshot: WorkspaceSnapshot,
  ): Result<WorkspaceImporterResult> {
    when (phase) {
      WorkspaceImporterPhase.Initialize -> {
        if (!scalaSdkExtensionExists()) {
          return Result.success(WorkspaceImporterResult.Abort)
        }
        scalaSdks = calculateAllScalaSdkInfos(snapshot)
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

  private fun calculateAllScalaSdkInfos(snapshot: WorkspaceSnapshot): Set<ScalaSdk> =
    snapshot.targets.mapNotNull { createScalaSdk(it.value.rawBuildTarget) }.toSet()

  private fun createScalaSdk(target: BuildTarget): ScalaSdk? =
    extractScalaBuildTarget(target)
      ?.let { scalaBuildTarget ->
        ScalaSdk(
          name = scalaBuildTarget.scalaVersion.scalaVersionToScalaSdkName(),
          scalaVersion = scalaBuildTarget.scalaVersion,
          sdkJars = scalaBuildTarget.sdkJars.map { path -> path.toUri() },
        )
      }
}
