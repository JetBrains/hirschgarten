package org.jetbrains.plugins.bsp.services

import com.intellij.openapi.project.Project
import com.intellij.project.stateStore
import com.intellij.workspaceModel.ide.WorkspaceModel
import com.intellij.workspaceModel.ide.getInstance
import com.intellij.workspaceModel.storage.url.VirtualFileUrlManager
import org.jetbrains.magicmetamodel.MagicMetaModel
import org.jetbrains.magicmetamodel.MagicMetaModelProjectConfig
import org.jetbrains.plugins.bsp.protocol.VeryTemporaryBspResolver

class MagicMetaModelService(project: Project) {

  val magicMetaModel = initializeMagicModel(project)

  private fun initializeMagicModel(project: Project): MagicMetaModel {
    val magicMetaModelProjectConfig = calculateProjectConfig(project)
    val bspResolver = VeryTemporaryBspResolver(magicMetaModelProjectConfig.projectBaseDir)
    val projectDetails = bspResolver.collectModel()

    return MagicMetaModel.create(magicMetaModelProjectConfig, projectDetails)
  }

  private fun calculateProjectConfig(project: Project): MagicMetaModelProjectConfig {
    val workspaceModel = WorkspaceModel.getInstance(project)
    val virtualFileUrlManager = VirtualFileUrlManager.getInstance(project)
    val projectBaseDir = project.stateStore.projectBasePath

    return MagicMetaModelProjectConfig(workspaceModel, virtualFileUrlManager, projectBaseDir)
  }

  companion object {
    fun getInstance(project: Project): MagicMetaModelService =
      project.getService(MagicMetaModelService::class.java)
  }
}
