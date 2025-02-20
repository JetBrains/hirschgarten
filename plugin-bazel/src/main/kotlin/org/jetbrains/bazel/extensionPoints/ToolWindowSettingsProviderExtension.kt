package org.jetbrains.bazel.extensionPoints

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.WithBuildToolId
import org.jetbrains.bazel.config.buildToolId
import org.jetbrains.bazel.config.withBuildToolId

interface ToolWindowSettingsProviderExtension : WithBuildToolId {
  fun getSettingsName(): String

  companion object {
    internal val ep =
      ExtensionPointName.create<ToolWindowSettingsProviderExtension>(
        "org.jetbrains.bazel.toolWindowSettingsProviderExtension",
      )
  }
}

val Project.toolWindowSettingsProvider: ToolWindowSettingsProviderExtension?
  get() = ToolWindowSettingsProviderExtension.ep.withBuildToolId(buildToolId)
