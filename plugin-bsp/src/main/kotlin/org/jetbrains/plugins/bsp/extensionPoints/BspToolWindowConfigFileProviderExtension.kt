package org.jetbrains.plugins.bsp.extensionPoints

import com.intellij.icons.AllIcons
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.WithBuildToolId
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.config.withBuildToolId
import java.nio.file.Path
import javax.swing.Icon

interface BspToolWindowConfigFileProviderExtension : WithBuildToolId {
  fun getConfigFileGenericName(): String

  fun getConfigFile(project: Project): Path?

  fun getConfigFileIcon(): Icon = AllIcons.FileTypes.Config

  companion object {
    internal val ep =
      ExtensionPointName.create<BspToolWindowConfigFileProviderExtension>(
        "org.jetbrains.bsp.bspToolWindowConfigFileProviderExtension",
      )
  }
}

val Project.bspToolWindowConfigFileProvider: BspToolWindowConfigFileProviderExtension?
  get() = BspToolWindowConfigFileProviderExtension.ep.withBuildToolId(buildToolId)
