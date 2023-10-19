package org.jetbrains.plugins.bsp.ui.notifications

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import javax.swing.Icon

/**
 * Facilitates displaying balloon notifications. Notifications will display as follows:
 * ```
 * --------------------------
 * | [icon] <build tool id>: [subtitle] |
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
   * @param title title of the notification
   * @param content main content of the notification
   * @param subtitle subtitle of the notification (none by default)
   * @param customIcon icon for the notification
   */
  public fun info(title: String, content: String, subtitle: String = "", customIcon: Icon): Unit =
    notify(title, content, subtitle, NotificationType.INFORMATION, customIcon)

  /**
   * Display a warning as a balloon notification
   *
   * @param title title of the notification
   * @param content main content of the notification
   * @param subtitle subtitle of the notification (none by default)
   */
  public fun warn(title: String, content: String, subtitle: String = ""): Unit =
    notify(title, content, subtitle, NotificationType.WARNING, null)

  /**
   * Display an error as a balloon notification
   *
   * @param title title of the notification
   * @param content main content of the notification
   * @param subtitle subtitle of the notification (none by default)
   */
  public fun error(title: String, content: String, subtitle: String = ""): Unit =
    notify(title, content, subtitle, NotificationType.ERROR, null)

  private fun notify(
    title: String,
    content: String,
    subtitle: String,
    notificationType: NotificationType,
    customIcon: Icon?,
  ) {
    val notification = Notification(BALLOON_ID, "<html>$content</html>", notificationType)
      .setTitle(title)
      .setSubtitle(subtitle)
      .let { if (customIcon != null) it.setIcon(customIcon) else it }
    Notifications.Bus.notify(notification)
  }
}

private const val BALLOON_ID = "BSP Plugin"
