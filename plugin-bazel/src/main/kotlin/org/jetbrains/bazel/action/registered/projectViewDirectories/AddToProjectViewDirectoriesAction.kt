package org.jetbrains.bazel.action.registered.projectViewDirectories

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class AddToProjectViewDirectoriesAction : AnAction() {
  val action: ProjectViewDirectoriesAction =
    ProjectViewDirectoriesAction(
      titleKey = "action.Bazel.AddToProjectViewDirectoriesAction.text",
      itemExists = ProjectViewDirectoriesAction.NotificationFactory(
        "Directory already included",
        { "Directory \'$it\' is already included in the project view file directories section." },
        NotificationType.INFORMATION,
      ),
      oppositeStateItemExists = ProjectViewDirectoriesAction.NotificationFactory(
        "Including excluded directory",
        { "Attempted to include directory \'$it\' which has been already excluded from the project view file directories section." },
        NotificationType.WARNING,
      ),
    )

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val selectedDirectoryRelativePath = action.getSelectedDirectoryRelativePath(project, e) ?: return
    action.addItemToProjectView(selectedDirectoryRelativePath, e)
  }

  override fun update(e: AnActionEvent) = action.update(e)

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
