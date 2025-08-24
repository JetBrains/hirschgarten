package org.jetbrains.bazel.action.registered.projectViewDirectories

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import org.jetbrains.bazel.languages.projectview.base.ProjectViewFileType
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import java.nio.file.Path
import java.nio.file.Paths

data class ProjectViewDirectoriesAction(
  val itemExists: NotificationFactory,
  /** Shown when the user attempts to add or exclude a directory that is already in the opposite state in the Project View file. */
  val oppositeStateItemExists: NotificationFactory,
) {
  data class NotificationFactory(
    val title: String,
    val content: (String) -> String,
    val type: NotificationType,
  ) {
    companion object {
      const val NOTIFICATION_GROUP_ID: String = "BazelPlugin"
    }

    fun create(directory: String): Notification = Notification(NOTIFICATION_GROUP_ID, title, content(directory), type)
  }

  fun update(event: AnActionEvent) {
    event.presentation.isEnabledAndVisible = event.project?.let { project ->
      !DumbService.isDumb(project) && getProjectViewPath(project) != null && getSelectedDirectoryRelativePath(project, event) != null
    } ?: false
  }

  fun getSelectedDirectoryRelativePath(project: Project, event: AnActionEvent): Path? {
    val selectedDirectoryPath = getSelectedDirectoryPath(event) ?: return null
    return getPathRelativeToProjectRoot(project, selectedDirectoryPath)
  }

  fun addItemToProjectView(item: String, event: AnActionEvent) {
    val project = event.project ?: return
    val newSelectedDirectoryPsi = createDirectoriesPsi(project, item)
    val projectViewPsi = getProjectViewPsiFile(project) ?: return
    val directoriesPsi = projectViewPsi.getSection("directories")
    if (directoriesPsi != null) {
      if (sectionContainsItem(item, directoriesPsi)) {
        notify(item, itemExists, project)
        return
      }
      if (sectionContainsItem(getOppositeStateItem(item), directoriesPsi)) {
        notify(item, oppositeStateItemExists, project)
        return
      }
      val colonPsi = newSelectedDirectoryPsi.getColon() ?: return
      WriteCommandAction.runWriteCommandAction(project) {
        directoriesPsi.addRangeAfter(colonPsi.nextSibling, newSelectedDirectoryPsi.getItems().first(), directoriesPsi.getItems().last())
      }
    } else {
      WriteCommandAction.runWriteCommandAction(project) {
        projectViewPsi.addBefore(newSelectedDirectoryPsi, projectViewPsi.firstChild)
      }
    }
  }

  private fun getOppositeStateItem(item: String) = if (item[0] == '-') item.substring(1) else "-$item"

  private fun getDirectory(item: String) = if (item[0] == '-') item.substring(1) else item

  private fun sectionContainsItem(item: String, section: ProjectViewPsiSection): Boolean =
    section.getItems().find { it.text == item } != null

  private fun getProjectViewPath(project: Project): Path? = project.bazelProjectSettings.projectViewPath

  private fun notify(
    item: String,
    notificationFactory: NotificationFactory,
    project: Project,
  ) {
    Notifications.Bus.notify(notificationFactory.create(getDirectory(item)), project)
  }

  private fun getSelectedDirectoryPath(event: AnActionEvent): Path? =
    event.getData(CommonDataKeys.VIRTUAL_FILE)?.takeIf { it.isDirectory }?.path?.let { path ->
      Paths.get(path)
    }

  private fun getPathRelativeToProjectRoot(project: Project, path: Path): Path? {
    val basePath = project.basePath ?: return null
    return Paths.get(basePath).relativize(path)
  }

  private fun getProjectViewPsiFile(project: Project): ProjectViewPsiFile? {
    val projectViewPath = getProjectViewPath(project) ?: return null
    val vFile = LocalFileSystem.getInstance().findFileByNioFile(projectViewPath) ?: return null
    return PsiManager.getInstance(project).findFile(vFile) as? ProjectViewPsiFile
  }

  private fun createDirectoriesPsi(project: Project, item: String): ProjectViewPsiSection =
    PsiFileFactory
      .getInstance(project)
      .createFileFromText(
        "dummy.bazelproject",
        ProjectViewFileType,
        "directories:\n  $item\n\n",
      ).getChildrenOfType<ProjectViewPsiSection>()
      .first()
}
