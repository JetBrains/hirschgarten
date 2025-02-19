package org.jetbrains.bazel.ui.projectTree

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.ExtensionPointName.Companion.create
import com.intellij.openapi.project.Project

val Project.treeStructureSettings: TreeStructureSettings?
  get() =
    TreeStructureSettingsProvider.EP_NAME.extensions
      .asSequence()
      .map { it.getTreeStructureSettings(this) }
      .firstOrNull()

interface TreeStructureSettingsProvider {
  fun getTreeStructureSettings(project: Project): TreeStructureSettings?

  companion object {
    val EP_NAME: ExtensionPointName<TreeStructureSettingsProvider> = create("org.jetbrains.bazel.treeStructureSettingsExtension")
  }
}

interface TreeStructureSettings {
  val showExcludedDirectoriesAsSeparateNode: Boolean
}
