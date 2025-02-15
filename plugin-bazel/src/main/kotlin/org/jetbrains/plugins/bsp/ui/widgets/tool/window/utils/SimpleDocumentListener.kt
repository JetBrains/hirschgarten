package org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils

import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

public class SimpleDocumentListener(private val onUpdate: () -> Unit) : DocumentListener {
  override fun insertUpdate(e: DocumentEvent?) {
    onUpdate()
  }

  override fun removeUpdate(e: DocumentEvent?) {
    onUpdate()
  }

  override fun changedUpdate(e: DocumentEvent?) {
    onUpdate()
  }
}
