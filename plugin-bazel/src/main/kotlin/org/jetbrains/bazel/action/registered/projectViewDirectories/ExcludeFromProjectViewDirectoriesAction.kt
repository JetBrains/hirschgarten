package org.jetbrains.bazel.action.registered.projectViewDirectories

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ExcludeFromProjectViewDirectoriesAction : AnAction() {
  val action: ProjectViewDirectoriesAction =
    ProjectViewDirectoriesAction(
      ProjectViewDirectoriesAction.NotificationFactory(
        "Directory already excluded",
        { "Directory \'$it\' is already excluded from the project view file directories section." },
        NotificationType.INFORMATION,
      ),
      ProjectViewDirectoriesAction.NotificationFactory(
        "Excluding included directory",
        { "Attempted to exclude directory \'$it\' which has been already included in the project view file directories section." },
        NotificationType.WARNING,
      ),
    )

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val selectedDirectoryRelativePath = action.getSelectedDirectoryRelativePath(project, e) ?: return
    val item = "-$selectedDirectoryRelativePath"
    action.addItemToProjectView(item, e)
  }

  override fun update(e: AnActionEvent) = action.update(e)

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
