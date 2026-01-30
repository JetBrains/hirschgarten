package org.jetbrains.bazel.languages.starlark.bazel.bzlmod

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.languages.starlark.StarlarkBundle


class RefreshBcrModulesAction : SuspendableAction(StarlarkBundle.message("bzlmod.action.refresh.modules")) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    runCatching {
      BazelModuleRegistryService.getInstance(project).refreshModuleNames()
    }.onSuccess {
      notify(
        project,
        StarlarkBundle.message("bzlmod.notification.bcr.title"),
        StarlarkBundle.message("bzlmod.notification.bcr.refresh.success"),
        NotificationType.INFORMATION,
      )
    }.onFailure { throwable ->
      notify(
        project,
        StarlarkBundle.message("bzlmod.notification.bcr.title"),
        StarlarkBundle.message("bzlmod.notification.bcr.refresh.failure", throwable.message ?: ""),
        NotificationType.WARNING,
      )
    }
  }

  private fun notify(
    project: Project,
    @NlsContexts.NotificationTitle title: String,
    @NlsContexts.NotificationContent content: String,
    type: NotificationType,
  ) {
    Notifications.Bus.notify(
      NotificationGroupManager
        .getInstance()
        .getNotificationGroup("BazelPlugin")
        .createNotification(title, content, type),
      project,
    )
  }
}
