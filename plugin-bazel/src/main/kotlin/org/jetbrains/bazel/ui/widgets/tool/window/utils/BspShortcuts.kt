package org.jetbrains.bazel.ui.widgets.tool.window.utils

import com.intellij.openapi.actionSystem.CustomShortcutSet
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * Custom keyboard shortcuts for BSP plugin's UI.
 * If a shortcut is present in [com.intellij.openapi.actionSystem.CommonShortcuts], it does not have to be defined here
 */
object BspShortcuts {
  /** Toggle case-sensitivity in target search */
  val matchCase = CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK))

  /** Toggle regex mode in target search */
  val regexMode = CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.ALT_MASK))

  /** Switch focus from the searchbar, down to the tree/list of targets */
  val fromSearchBarToTargets = CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0))

  /** Open context menu for the selected target */
  val openTargetContextMenu = CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0))
}
