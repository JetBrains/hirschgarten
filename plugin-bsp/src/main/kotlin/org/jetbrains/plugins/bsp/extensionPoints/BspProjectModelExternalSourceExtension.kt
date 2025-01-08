package org.jetbrains.plugins.bsp.extensionPoints

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectModelExternalSource
import org.jetbrains.plugins.bsp.config.WithBuildToolId
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.config.withBuildToolId

interface BspProjectModelExternalSourceExtension :
  ProjectModelExternalSource,
  WithBuildToolId {
  companion object {
    val ep: ExtensionPointName<BspProjectModelExternalSourceExtension> =
      ExtensionPointName.create("org.jetbrains.bsp.bspProjectModelExternalSource")
  }
}

val Project.bspProjectModelExternalSource: ProjectModelExternalSource?
  get() =
    BspProjectModelExternalSourceExtension.ep.withBuildToolId(buildToolId)
