package org.jetbrains.bazel.extensionPoints

import com.intellij.icons.AllIcons
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.WithBuildToolId
import org.jetbrains.bazel.config.buildToolId
import org.jetbrains.bazel.config.withBuildToolId
import java.nio.file.Path
import javax.swing.Icon

interface BspToolWindowConfigFileProviderExtension : WithBuildToolId {
  fun getConfigFileGenericName(): String

  fun getConfigFile(project: Project): Path?

  fun getConfigFileIcon(): Icon = AllIcons.FileTypes.Config

  companion object {
    internal val ep =
      ExtensionPointName.create<BspToolWindowConfigFileProviderExtension>(
        "org.jetbrains.bazel.bspToolWindowConfigFileProviderExtension",
      )
  }
}

val Project.bspToolWindowConfigFileProvider: BspToolWindowConfigFileProviderExtension?
  get() = BspToolWindowConfigFileProviderExtension.ep.withBuildToolId(buildToolId)
