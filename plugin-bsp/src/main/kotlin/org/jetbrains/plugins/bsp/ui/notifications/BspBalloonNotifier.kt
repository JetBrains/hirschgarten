package org.jetbrains.plugins.bsp.ui.notifications

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import javax.swing.Icon

/**
 * Facilitates displaying balloon notifications. Notifications will display as follows:
 * ```
 * --------------------------
 * | [icon] BSP: [subtitle] |
 * |        [content]       |
 * --------------------------
 * ```
 * Custom icons are used only in `info(...)`; BSP icon is used by default.
 *
 * In `warn(...)` and `error(...)` default icons are used, because
 * the icon is the only difference between error, warning and information.
 *
 * If subtitle is empty, there will be no `':'` character after "BSP".
 */
public object BspBalloonNotifier {
  /**
   * Display an information as a balloon notification
   *
   * @param content main content of the notification
   * @param subtitle subtitle of the notification (none by default)
   * @param customIcon icon for the notification (BSP Plugin icon by default)
   */
  public fun info(content: String, subtitle: String = "", customIcon: Icon = BSP_ICON): Unit =
    notify(content, subtitle, NotificationType.INFORMATION, customIcon)

  /**
   * Display a warning as a balloon notification
   *
   * @param content main content of the notification
   * @param subtitle subtitle of the notification (none by default)
   */
  public fun warn(content: String, subtitle: String = ""): Unit =
    notify(content, subtitle, NotificationType.WARNING, null)

  /**
   * Display an error as a balloon notification
   *
   * @param content main content of the notification
   * @param subtitle subtitle of the notification (none by default)
   */
  public fun error(content: String, subtitle: String = ""): Unit =
    notify(content, subtitle, NotificationType.ERROR, null)

  private fun notify(
    content: String,
    subtitle: String,
    notificationType: NotificationType,
    customIcon: Icon?,
  ) {
    val notification = Notification(BALLOON_ID, "<html>$content</html>", notificationType)
      .setTitle(BALLOON_TITLE)
      .setSubtitle(subtitle)
      .let { if (customIcon != null) it.setIcon(customIcon) else it }
    Notifications.Bus.notify(notification)
  }
}

private val BSP_ICON = BspPluginIcons.bsp
private const val BALLOON_ID = "BSP Plugin"
private const val BALLOON_TITLE = "BSP"
