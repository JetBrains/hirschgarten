package org.jetbrains.bazel.flow.open

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import org.jetbrains.plugins.bsp.flow.open.BaseBspProjectOpenProcessor
import org.jetbrains.plugins.bsp.flow.open.BspProjectOpenProcessorExtension
import javax.swing.Icon

internal class BazelBspProjectOpenProcessor : BaseBspProjectOpenProcessor(bazelBspBuildToolId) {
  override val icon: Icon = BazelPluginIcons.bazel

  override val name: String = "Bazel"

  override fun canOpenProject(file: VirtualFile): Boolean =
    file.children.any { it.name in BazelPluginConstants.WORKSPACE_FILE_NAMES }
}

internal class BazelBspProjectOpenProcessorExtension : BspProjectOpenProcessorExtension {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override val shouldBspProjectOpenProcessorBeAvailable: Boolean = false
}