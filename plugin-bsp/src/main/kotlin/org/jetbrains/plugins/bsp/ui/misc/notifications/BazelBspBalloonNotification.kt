package org.jetbrains.plugins.bsp.ui.misc.notifications

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import org.jetbrains.plugins.bsp.config.BspPluginIcons

public class BazelBspBalloonNotification(
  content: String,
  contentType: NotificationType = NotificationType.INFORMATION
): Notification("Bazel BSP", content, contentType) {

  init {
    setIcon(BspPluginIcons.bazel)
  }
}
