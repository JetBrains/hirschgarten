package org.jetbrains.bazel.kotlin.startup

import com.intellij.diagnostic.VMOptions
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.Project
import com.intellij.util.PlatformUtils
import org.jetbrains.bazel.config.BazelFeatureFlags.isKotlinPluginK2Mode
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.startup.utils.BazelProjectActivity
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.configuration.ui.KotlinPluginKindSwitcherController
import org.jetbrains.kotlin.idea.configuration.ui.USE_K2_PLUGIN_VM_OPTION_PREFIX

/**
 * Based on org.jetbrains.kotlin.onboarding.k2.EnableK2NotificationService
 */
private class SuggestEnableK2StartupActivity : BazelProjectActivity() {
  override suspend fun executeForBazelProject(project: Project) {
    if (isKotlinPluginK2Mode || PlatformUtils.isCLion()) return

    serviceAsync<NotificationGroupManager>()
      .getNotificationGroup(BazelPluginBundle.message("post.startup.activity.id.enable.k2.compiler"))
      .createNotification(
        BazelPluginBundle.message("widget.suggest.enable.k2.title"),
        BazelPluginBundle.message("widget.suggest.enable.k2.message"),
        NotificationType.INFORMATION,
      ).setIcon(KotlinIcons.SMALL_LOGO)
      .addAction(
        NotificationAction.createExpiring(BazelPluginBundle.message("widget.suggest.enable.k2.action")) { _, _ ->
          VMOptions.setOption(USE_K2_PLUGIN_VM_OPTION_PREFIX, true.toString())
          KotlinPluginKindSwitcherController.suggestRestart(ApplicationNamesInfo.getInstance().fullProductName)
        },
      ).notify(project)
  }
}
