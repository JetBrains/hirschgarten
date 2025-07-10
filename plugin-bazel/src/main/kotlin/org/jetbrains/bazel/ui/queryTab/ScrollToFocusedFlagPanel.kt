package org.jetbrains.bazel.ui.queryTab

import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.KeyboardFocusManager
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.SwingUtilities

internal class ScrollToFocusedFlagPanel(private val component: JComponent) : JBScrollPane(component) {
  init {
    border = JBUI.Borders.empty()
    minimumSize = Dimension(0, 0)
    val focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
    val listener = PropertyChangeListener { e ->
      if (e.newValue != null) {
        val focused = focusManager.focusOwner
        if (focused != null && SwingUtilities.isDescendingFrom(focused, component)) {
          SwingUtilities.invokeLater {
            when (focused) {
              is LanguageTextField -> {
                val componentOfFocused = focused.component
                val bounds = SwingUtilities.convertRectangle(
                  componentOfFocused.parent,
                  componentOfFocused.bounds,
                  viewport
                )
                viewport.scrollRectToVisible(bounds)
              }
              is JComponent -> {
                val bounds = SwingUtilities.convertRectangle(
                  focused.parent,
                  focused.bounds,
                  viewport
                )
                viewport.scrollRectToVisible(bounds)
              }
            }
          }
        }
      }
    }
    focusManager.addPropertyChangeListener("focusOwner", listener)
  }
}

