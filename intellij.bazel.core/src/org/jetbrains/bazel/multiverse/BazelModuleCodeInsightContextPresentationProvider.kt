package org.jetbrains.bazel.multiverse

import com.intellij.codeInsight.multiverse.CodeInsightContext
import com.intellij.codeInsight.multiverse.CodeInsightContextPresentationProvider
import com.intellij.codeInsight.multiverse.ModuleContext
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.workspaceModel.ide.legacyBridge.findModuleEntity
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Nls
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.workspacemodel.entities.bazelModuleExtension
import org.jetbrains.bazel.workspacemodel.entities.targetKey
import javax.swing.Icon

internal class BazelModuleCodeInsightContextPresentationProvider : CodeInsightContextPresentationProvider<ModuleContext> {
  override fun isApplicable(context: CodeInsightContext): Boolean =
    context is ModuleContext && context.getModule()?.findModuleEntity()?.bazelModuleExtension != null

  override fun getIcon(
    context: ModuleContext,
    project: Project,
  ): Icon = BazelPluginIcons.bazel

  override fun getPresentableText(
    context: ModuleContext,
    project: Project,
  ): @Nls String {
    val module = context.getModule()?.findModuleEntity() ?: return ""
    val bazelEntity = module.bazelModuleExtension ?: return ""
    val snapshot = project.service<WorkspaceSnapshotService>().snapshot.value
    val targetKey = bazelEntity.targetKey
    @NlsSafe val label = targetKey.label.toString()
    @NlsSafe val configurationName = snapshot.configurationName(targetKey)
    return if (configurationName == null) label else "$label $configurationName"
  }

  @ApiStatus.Internal
  private fun WorkspaceSnapshot.configurationName(key: WorkspaceTargetKey): String? {
    val mnemonic = this.configurations[key.configuration]?.summary?.mnemonic
    val checksum = key.configuration.shortChecksum
    return when {
      checksum == null -> null
      mnemonic == null -> checksum
      else -> "$mnemonic $checksum"
    }
  }
}
