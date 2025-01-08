package org.jetbrains.plugins.bsp.extensionPoints

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.WithBuildToolId
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.config.withBuildToolId

interface BspToolWindowSettingsProviderExtension : WithBuildToolId {
  fun getSettingsName(): String

  companion object {
    internal val ep =
      ExtensionPointName.create<BspToolWindowSettingsProviderExtension>(
        "org.jetbrains.bsp.bspToolWindowSettingsProviderExtension",
      )
  }
}

val Project.bspToolWindowSettingsProvider: BspToolWindowSettingsProviderExtension?
  get() = BspToolWindowSettingsProviderExtension.ep.withBuildToolId(buildToolId)
