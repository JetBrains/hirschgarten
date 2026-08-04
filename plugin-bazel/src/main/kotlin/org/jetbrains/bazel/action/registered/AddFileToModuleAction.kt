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
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.sync.status.isSyncInProgress
import org.jetbrains.bazel.target.moduleEntity
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.utils.isSourceFile
import org.jetbrains.bazel.workspace.addToModule
import org.jetbrains.bazel.workspace.askForInverseSources
import org.jetbrains.bazel.workspace.getModulesForFile
import org.jetbrains.bazel.workspace.processTargetsForTestlibStripping
import org.jetbrains.bazel.workspace.resolvePackagePrefix
import org.jetbrains.bazel.workspace.toModuleEntity
import org.jetbrains.bazel.workspacemodel.entities.BazelDummyEntitySource

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
          .toSet()
        val url = virtualFile.toVirtualFileUrl(workspaceModel.getVirtualFileUrlManager())
        val path = url.toPath()

        // Query Bazel for targets that should contain this file
        val targets = reporter.nextStep(
          endFraction = 80,
          text = BazelPluginBundle.message("file.change.processing.step.query"),
        ) {
          try {
            askForInverseSources(project, url).targets.values.flatten()
          } catch (ex: Exception) {
            emptyList()
          }
        }

        if (targets.isNotEmpty()) {
          val processedResult = processTargetsForTestlibStripping(targets)
          val modulesWithTestFlag = processedResult.allProcessedTargets.mapNotNull {
            it.toModuleEntity(workspaceModel.currentSnapshot, entityStorageDiff, project)
          }

          val moduleNameToLabel = processedResult.allProcessedTargets.associateBy { it.formatAsModuleName(project) }
          // Add file only to non-stripped targets (original .testlib targets)
          for ((module, isTestModule) in modulesWithTestFlag) {
            val moduleLabel = moduleNameToLabel[module.name]
            val isStripped = moduleLabel != null && processedResult.strippedLabels.contains(moduleLabel)
            val alreadyAdded = existingModules.contains(module)
            if (!alreadyAdded && !isStripped) {
              val parentUrl = virtualFile.parent?.toVirtualFileUrl(workspaceModel.getVirtualFileUrlManager())
              val packagePrefix = resolvePackagePrefix(parentUrl, module)
              url.addToModule(entityStorageDiff, module, virtualFile.extension, isTestModule, packagePrefix)
            }
          }

          // Store only stripped targets (not original .testlib) in target utils mapping
          project.targetUtils.addFileToTargetIdEntry(path, processedResult.targetsForMapping)
          reporter.nextStep(endFraction = 100, text = BazelPluginBundle.message("file.change.processing.step.commit")) {
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
