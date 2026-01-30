package org.jetbrains.bazel.projectAware

import com.intellij.openapi.externalSystem.ui.ExternalSystemIconProvider
import org.jetbrains.bazel.assets.BazelPluginIcons
import javax.swing.Icon

class BazelExternalSystemIconProvider : ExternalSystemIconProvider {
  override val reloadIcon: Icon
    get() = BazelPluginIcons.bazelReload
}
