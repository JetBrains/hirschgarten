package org.jetbrains.bazel.ui.widgets.tool.window.utils

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class SimpleAction(private val action: () -> Unit) : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    action()
  }
}
