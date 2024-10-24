package org.jetbrains.bazel.flow.open

import com.intellij.diagnostic.VMOptions
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.configuration.ui.KotlinPluginKindSwitcherController
import org.jetbrains.kotlin.idea.configuration.ui.USE_K2_PLUGIN_VM_OPTION_PREFIX
import org.jetbrains.plugins.bsp.config.BspFeatureFlags.isKotlinPluginK2Mode
import org.jetbrains.plugins.bsp.startup.BspProjectActivity

private const val SUGGEST_ENABLE_K2_NOTIFICATION_GROUP = "Enable K2 Compiler Mode with Bazel"

/**
 * Based on org.jetbrains.kotlin.onboarding.k2.EnableK2NotificationService
 */
class SuggestEnableK2StartupActivity : BspProjectActivity() {
  override suspend fun Project.executeForBspProject() {
    if (isKotlinPluginK2Mode) return

    NotificationGroupManager
      .getInstance()
      .getNotificationGroup(SUGGEST_ENABLE_K2_NOTIFICATION_GROUP)
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
      ).notify(this)
  }
}
