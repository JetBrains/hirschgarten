package org.jetbrains.bazel.extension

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.bazel.settings.bazelProjectSettings
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.extensionPoints.BspToolWindowConfigFileProviderExtension
import java.nio.file.Path
import javax.swing.Icon

class BazelToolWindowConfigFileProviderExtension : BspToolWindowConfigFileProviderExtension {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override fun getConfigFileGenericName(): String = BazelPluginBundle.message("tool.window.generic.config.file")

  override fun getConfigFile(project: Project): Path? = project.bazelProjectSettings.projectViewPath

  override fun getConfigFileIcon(): Icon = BazelPluginIcons.bazel
}
