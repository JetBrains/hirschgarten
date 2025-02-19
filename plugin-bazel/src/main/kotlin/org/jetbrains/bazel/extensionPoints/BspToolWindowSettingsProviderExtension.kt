package org.jetbrains.bazel.extensionPoints

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.WithBuildToolId
import org.jetbrains.bazel.config.buildToolId
import org.jetbrains.bazel.config.withBuildToolId

interface BspToolWindowSettingsProviderExtension : WithBuildToolId {
  fun getSettingsName(): String

  companion object {
    internal val ep =
      ExtensionPointName.create<BspToolWindowSettingsProviderExtension>(
        "org.jetbrains.bazel.bspToolWindowSettingsProviderExtension",
      )
  }
}

val Project.bspToolWindowSettingsProvider: BspToolWindowSettingsProviderExtension?
  get() = BspToolWindowSettingsProviderExtension.ep.withBuildToolId(buildToolId)
