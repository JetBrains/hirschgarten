package org.jetbrains.plugins.bsp.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import org.jetbrains.plugins.bsp.services.MagicMetaModelService

internal class MyProjectManagerListener : ProjectManagerListener {

  override fun projectOpened(project: Project) {
    project.service<MagicMetaModelService>()
  }
}
