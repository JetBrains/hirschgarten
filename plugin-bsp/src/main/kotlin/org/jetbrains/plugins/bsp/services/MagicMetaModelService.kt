package org.jetbrains.plugins.bsp.services

import com.intellij.openapi.project.Project
import com.intellij.project.stateStore
import com.intellij.workspaceModel.ide.WorkspaceModel
import com.intellij.workspaceModel.ide.getInstance
import com.intellij.workspaceModel.storage.url.VirtualFileUrlManager
import org.jetbrains.magicmetamodel.MagicMetaModel
import org.jetbrains.magicmetamodel.MagicMetaModelProjectConfig

public class MagicMetaModelService(private val project: Project) {

  public lateinit var magicMetaModel: MagicMetaModel

  private val bspConnectionService = BspConnectionService.getInstance(project)

  public fun initializeMagicModel() {
    val magicMetaModelProjectConfig = calculateProjectConfig(project)
    val projectDetails = bspConnectionService.bspResolver!!.collectModel()

    magicMetaModel = MagicMetaModel.create(magicMetaModelProjectConfig, projectDetails)
  }

  private fun calculateProjectConfig(project: Project): MagicMetaModelProjectConfig {
    val workspaceModel = WorkspaceModel.getInstance(project)
    val virtualFileUrlManager = VirtualFileUrlManager.getInstance(project)
    val projectBaseDir = project.stateStore.projectBasePath

    return MagicMetaModelProjectConfig(workspaceModel, virtualFileUrlManager, projectBaseDir)
  }

  public companion object {
    public fun getInstance(project: Project): MagicMetaModelService =
      project.getService(MagicMetaModelService::class.java)
  }
}
