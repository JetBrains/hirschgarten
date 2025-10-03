package org.jetbrains.bazel.action.registered

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.workspaceModel.ide.toPath
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.BazelDummyEntitySource
import org.jetbrains.bazel.sync.status.isSyncInProgress
import org.jetbrains.bazel.target.moduleEntity
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.utils.isSourceFile
import org.jetbrains.bazel.workspace.addToModule
import org.jetbrains.bazel.workspace.askForInverseSources
import org.jetbrains.bazel.workspace.getModulesForFile
import org.jetbrains.bazel.workspace.toModuleEntity

class AddFileToModuleAction :
  SuspendableAction({ BazelPluginBundle.message("add.file.to.module.action.text") }, BazelPluginIcons.bazel) {

  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

    val workspaceModel = project.serviceAsync<WorkspaceModel>()
    val entityStorageDiff = MutableEntityStorage.from(workspaceModel.currentSnapshot)

    withBackgroundProgress(project, BazelPluginBundle.message("add.file.to.module.action.progress", virtualFile.name)) {
      reportSequentialProgress { reporter ->
        // Get existing modules for the file
        val existingModules = getModulesForFile(virtualFile, project)
          .filter { it.moduleEntity?.entitySource != BazelDummyEntitySource }
          .mapNotNull { it.moduleEntity }

        val url = virtualFile.toVirtualFileUrl(workspaceModel.getVirtualFileUrlManager())
        val path = url.toPath()

        // Query Bazel for targets that should contain this file
        val targets = reporter.nextStep(
          endFraction = 80,
          text = BazelPluginBundle.message("file.change.processing.step.query"),
        ) {
          try {
            askForInverseSources(project, url).targets.toList()
          } catch (ex: Exception) {
            emptyList() // If query fails, return empty list
          }
        }

        if (targets.isNotEmpty()) {
          // Convert targets to module entities and add the file
          val modulesWithTestFlag = targets.mapNotNull { it.toModuleEntity(workspaceModel.currentSnapshot, entityStorageDiff, project) }

          for ((module, isTestModule) in modulesWithTestFlag) {
            val alreadyAdded = existingModules.contains(module)
            if (!alreadyAdded) {
              url.addToModule(entityStorageDiff, module, virtualFile.extension, isTestModule)
            }
          }

          // Update the target utils mapping
          project.targetUtils.addFileToTargetIdEntry(path, targets)

          reporter.nextStep(endFraction = 100, text = BazelPluginBundle.message("file.change.processing.step.commit")) {
            // Apply changes to workspace model
            workspaceModel.update("Add file to module (Bazel)") {
              it.applyChangesFrom(entityStorageDiff)
            }
          }
        }
      }
    }
  }

  override fun update(project: Project, e: AnActionEvent) {
    val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)

    // Show action only for Bazel projects, source files, and when sync is not in progress
    val isVisible = project.isBazelProject &&
                   virtualFile != null &&
                   !virtualFile.isDirectory &&
                   virtualFile.isSourceFile()

    val isEnabled = isVisible && !project.isSyncInProgress()

    e.presentation.isVisible = isVisible
    e.presentation.isEnabled = isEnabled

    // Update text based on file
    if (virtualFile != null) {
      e.presentation.text = BazelPluginBundle.message("add.file.to.module.action.text.with.file", virtualFile.name)
    }
  }
}
