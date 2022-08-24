package org.jetbrains.plugins.bsp.services

import com.intellij.openapi.project.Project
import com.intellij.project.stateStore
import com.intellij.workspaceModel.ide.WorkspaceModel
import com.intellij.workspaceModel.ide.getInstance
import com.intellij.workspaceModel.storage.url.VirtualFileUrlManager
import org.jetbrains.magicmetamodel.MagicMetaModel
import org.jetbrains.magicmetamodel.MagicMetaModelProjectConfig
import org.jetbrains.magicmetamodel.ProjectDetails

public class MagicMetaModelService(private val project: Project) {

  public lateinit var magicMetaModel: MagicMetaModel

  public fun initializeMagicModel(projectDetails: ProjectDetails) {
    val magicMetaModelProjectConfig = calculateProjectConfig(project)

    magicMetaModel = MagicMetaModel.create(magicMetaModelProjectConfig, projectDetails)
  }

  private fun calculateProjectConfig(project: Project): MagicMetaModelProjectConfig {
    val workspaceModel = WorkspaceModel.getInstance(project)
    val virtualFileUrlManager = VirtualFileUrlManager.getInstance(project)

    return MagicMetaModelProjectConfig(workspaceModel, virtualFileUrlManager)
  }

  public companion object {
    public fun getInstance(project: Project): MagicMetaModelService =
      project.getService(MagicMetaModelService::class.java)
  }
}
