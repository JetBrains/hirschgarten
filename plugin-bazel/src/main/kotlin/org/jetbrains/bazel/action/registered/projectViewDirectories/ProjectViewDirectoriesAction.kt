package org.jetbrains.bazel.action.registered.projectViewDirectories

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import org.jetbrains.annotations.PropertyKey
import org.jetbrains.bazel.config.BUNDLE
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.base.ProjectViewFileType
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

data class ProjectViewDirectoriesAction(
  @param:PropertyKey(resourceBundle = BUNDLE)
  val titleKey: String,
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
    val projectViewPath = getProjectViewPathIfActionApplicableTo(event)
    if (projectViewPath == null) {
      event.presentation.isEnabledAndVisible = false
      return
    }
    val text = BazelPluginBundle.message(titleKey, projectViewPath.name)
    event.presentation.isEnabledAndVisible = true
    event.presentation.setText(text)
  }

  fun getSelectedDirectoryRelativePath(project: Project, event: AnActionEvent): String? {
    val selectedDirectory = getSelectedDirectory(event) ?: return null
    return VfsUtil.getRelativePath(selectedDirectory, project.rootDir)
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
        directoriesPsi.addRangeAfter(
          colonPsi.nextSibling,
          newSelectedDirectoryPsi.getItems().first(),
          directoriesPsi.getItems().lastOrNull() ?: directoriesPsi.getColon(),
        )
      }
    } else {
      WriteCommandAction.runWriteCommandAction(project) {
        projectViewPsi.addBefore(newSelectedDirectoryPsi, projectViewPsi.firstChild)
      }
    }
  }

  private fun getProjectViewPathIfActionApplicableTo(event: AnActionEvent): VirtualFile? {
    val project = event.project ?: return null
    if (DumbService.isDumb(project)) return null
    val projectViewPath = getProjectViewPath(project) ?: return null
    if (getSelectedDirectoryRelativePath(project, event) == null) return null
    return projectViewPath
  }

  private fun getOppositeStateItem(item: String) = if (item[0] == '-') item.substring(1) else "-$item"

  private fun getDirectory(item: String) = if (item[0] == '-') item.substring(1) else item

  private fun sectionContainsItem(item: String, section: ProjectViewPsiSection): Boolean =
    section.getItems().find { it.text == item } != null

  private fun getProjectViewPath(project: Project): VirtualFile? = project.bazelProjectSettings.projectViewPath

  private fun notify(
    item: String,
    notificationFactory: NotificationFactory,
    project: Project,
  ) {
    Notifications.Bus.notify(notificationFactory.create(getDirectory(item)), project)
  }

  private fun getSelectedDirectory(event: AnActionEvent): VirtualFile? = event
    .getData(CommonDataKeys.VIRTUAL_FILE)
    ?.takeIf { it.isDirectory }

  private fun getProjectViewPsiFile(project: Project): ProjectViewPsiFile? {
    val vFile = getProjectViewPath(project) ?: return null
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
