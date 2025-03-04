package org.jetbrains.bazel.run.config

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.config.BazelPluginConstants.BAZEL_DISPLAY_NAME
import org.jetbrains.bazel.config.BspPluginBundle

class BazelRunConfigurationType :
  SimpleConfigurationType(
    id = ID,
    icon =
      NotNullLazyValue.createValue {
        BazelPluginIcons.bazel
      },
    name = BAZEL_DISPLAY_NAME,
    description = BspPluginBundle.message("runconfig.run.description", BAZEL_DISPLAY_NAME),
  ),
  DumbAware {
  override fun createTemplateConfiguration(project: Project): RunConfiguration = BazelRunConfiguration(project, "", this)

  override fun isEditableInDumbMode(): Boolean = true

  companion object {
    const val ID: String = "BazelRunConfigurationType"
  }
}
