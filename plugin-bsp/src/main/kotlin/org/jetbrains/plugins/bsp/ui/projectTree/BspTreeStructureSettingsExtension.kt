package org.jetbrains.plugins.bsp.ui.projectTree

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.ExtensionPointName.Companion.create
import com.intellij.openapi.project.Project

val Project.bspTreeStructureSettings: BspTreeStructureSettings?
  get() =
    BspTreeStructureSettingsProvider.EP_NAME.extensions
      .asSequence()
      .map { it.getBspTreeStructureSettings(this) }
      .firstOrNull()

interface BspTreeStructureSettingsProvider {
  fun getBspTreeStructureSettings(project: Project): BspTreeStructureSettings?

  companion object {
    val EP_NAME: ExtensionPointName<BspTreeStructureSettingsProvider> = create("org.jetbrains.bsp.bspTreeStructureSettingsExtension")
  }
}

interface BspTreeStructureSettings {
  val showExcludedDirectoriesAsSeparateNode: Boolean
}
