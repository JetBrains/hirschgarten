package org.jetbrains.bazel.extension

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.bazel.config.BuildToolId
import org.jetbrains.bazel.extensionPoints.BspToolWindowConfigFileProviderExtension
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import java.nio.file.Path

class BazelToolWindowConfigFileProviderExtension : BspToolWindowConfigFileProviderExtension {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override fun getConfigFileGenericName(): String = BazelPluginBundle.message("tool.window.generic.config.file")

  override fun getConfigFile(project: Project): Path? = project.bazelProjectSettings.projectViewPath
}
