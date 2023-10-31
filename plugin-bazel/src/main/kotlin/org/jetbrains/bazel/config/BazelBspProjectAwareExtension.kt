package org.jetbrains.bazel.config

import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectId
import com.intellij.openapi.externalSystem.ui.ExternalSystemIconProvider
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.plugins.bsp.config.BspProjectAwareExtension
import javax.swing.Icon

class BazelBspProjectAwareExtension : BspProjectAwareExtension {
  override fun getProjectId(projectPath: VirtualFile): ExternalSystemProjectId =
    ExternalSystemProjectId(BazelPluginConstants.SYSTEM_ID, projectPath.path)

  override val eligibleConfigFileNames: List<String> =
    BazelPluginConstants.SUPPORTED_CONFIG_FILE_NAMES

  override val eligibleConfigFileExtensions: List<String> =
    BazelPluginConstants.SUPPORTED_EXTENSIONS
}

class BazelExternalSystemIconProvider: ExternalSystemIconProvider {
  override val reloadIcon: Icon
    get() = BazelPluginIcons.bazelReload
}
