package com.intellij.bazel.devkit.monorepo.sync

import com.intellij.monorepo.devkit.bazel.isMonorepoProject
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectId
import com.intellij.openapi.externalSystem.autolink.UnlinkedProjectNotificationAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.jetbrains.bazel.config.BazelPluginConstants

internal class UnlinkedProjectNotificationDisabler : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (!isMonorepoProject(project)) return
    val notificationAware = UnlinkedProjectNotificationAware.getInstance(project)
    val state: UnlinkedProjectNotificationAware.State = notificationAware.state
    if (BazelPluginConstants.SYSTEM_ID.id in state.disabledNotifications) return

    val newState = state.copy(disabledNotifications = state.disabledNotifications + setOf(BazelPluginConstants.SYSTEM_ID.id))
    notificationAware.loadState(newState)
    notificationAware.notificationExpire(ExternalSystemProjectId(BazelPluginConstants.SYSTEM_ID, project.basePath ?: return))
  }
}
