package org.jetbrains.bazel.debug.configuration

import com.intellij.openapi.options.SettingsEditor
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.bazel.config.BazelPluginBundle
import javax.swing.JComponent

internal class StarlarkDebugSettingsEditor : SettingsEditor<StarlarkDebugConfiguration>() {
  private val targetField = JBTextField()

  override fun resetEditorFrom(config: StarlarkDebugConfiguration) {
    targetField.text = config.getTarget()
  }

  override fun applyEditorTo(config: StarlarkDebugConfiguration) {
    config.setTarget(targetField.text)
  }

  override fun createEditor(): JComponent =
    panel {
      row(BazelPluginBundle.message("starlark.debug.config.target.label")) {
        cell(targetField)
          .align(AlignX.FILL)
          .resizableColumn()
      }
    }
}
