package org.jetbrains.bazel.flow.open

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.plugins.bsp.flow.open.BaseBspProjectOpenProcessor
import org.jetbrains.plugins.bsp.flow.open.BspProjectOpenProcessorExtension
import org.jetbrains.plugins.bsp.flow.open.BuildToolId
import javax.swing.Icon

internal class BazelBspProjectOpenProcessor : BaseBspProjectOpenProcessor(BuildToolId("bazelbsp")) {
  override val icon: Icon = BazelPluginIcons.bazel

  override val name: String = "Bazel"

  override fun canOpenProject(file: VirtualFile): Boolean =
    file.children.any { it.name in BazelPluginConstants.WORKSPACE_FILE_NAMES }
}

internal class BazelBspProjectOpenProcessorExtension : BspProjectOpenProcessorExtension {
  override val buildToolId: BuildToolId = BuildToolId("bazelbsp")

  override val shouldBspProjectOpenProcessorBeAvailable: Boolean = false
}