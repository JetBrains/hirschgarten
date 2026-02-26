package org.jetbrains.bazel.action.registered

import com.intellij.ide.actions.RevealFileAction
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import org.jetbrains.bazel.bazelrunner.BazelLog
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject
import java.nio.file.Files

class ShowBazelLogAction : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val logFile = BazelLog.logPath
    if (Files.exists(logFile)) {
      RevealFileAction.openFile(logFile)
    }
  }

  override fun update(e: AnActionEvent) {
    val isBazel = e.project?.isBazelProject == true
    val logExists = Files.exists(BazelLog.logPath)
    e.presentation.isEnabledAndVisible = isBazel && logExists && RevealFileAction.isSupported()
    if (e.presentation.isEnabledAndVisible) {
      e.presentation.text = BazelPluginBundle.message(
        "action.Bazel.ShowBazelLogAction.text",
        RevealFileAction.getFileManagerName(),
      )
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
