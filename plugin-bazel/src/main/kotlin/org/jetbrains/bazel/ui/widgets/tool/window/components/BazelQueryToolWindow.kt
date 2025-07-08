package org.jetbrains.bazel.ui.widgets.tool.window.components

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import org.jetbrains.bazel.ui.queryTab.BazelQueryTab

class BazelQueryToolWindow(val project: Project) : SimpleToolWindowPanel(false, true) {
  init {
    setContent(BazelQueryTab(project))
    updateUI()
  }
}
