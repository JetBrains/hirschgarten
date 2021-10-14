package org.jetbrains.plugins.bsp.services

import com.intellij.openapi.project.Project
import com.intellij.project.stateStore
import com.intellij.workspaceModel.ide.WorkspaceModel
import com.intellij.workspaceModel.ide.getInstance
import com.intellij.workspaceModel.storage.url.VirtualFileUrlManager
import org.jetbrains.magicmetamodel.MagicMetaModel
import org.jetbrains.magicmetamodel.MagicMetaModelProjectConfig
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.plugins.bsp.startup.SampleBSPProjectToImport

class MagicMetaModelService(project: Project) {

  val magicMetaModel: MagicMetaModel

  // TODO extract
  init {
    val workspaceModel = WorkspaceModel.getInstance(project)
    val virtualFileUrlManager = VirtualFileUrlManager.getInstance(project)
    val projectBaseDir = project.stateStore.projectBasePath

    val magicMetaModelProjectConfig = MagicMetaModelProjectConfig(workspaceModel, virtualFileUrlManager, projectBaseDir)

    val projectDetails = ProjectDetails(
      targetsId = SampleBSPProjectToImport.allTargetsIds,
      targets = listOf(
        SampleBSPProjectToImport.libA1Target,
        SampleBSPProjectToImport.appTarget,
        SampleBSPProjectToImport.libBB2Target,
        SampleBSPProjectToImport.libBBTarget,
        SampleBSPProjectToImport.libA2Target,
        SampleBSPProjectToImport.anotherAppTarget,
        SampleBSPProjectToImport.libBTarget,
      ),
      sources = listOf(
        SampleBSPProjectToImport.libBB2Sources,
        SampleBSPProjectToImport.libA2Sources,
        SampleBSPProjectToImport.anotherAppSources,
        SampleBSPProjectToImport.libA1Sources,
        SampleBSPProjectToImport.libBBSources,
        SampleBSPProjectToImport.libBSources,
        SampleBSPProjectToImport.appSources,
      ),
      resources = listOf(
        SampleBSPProjectToImport.appResources,
        SampleBSPProjectToImport.libBBResources,
        SampleBSPProjectToImport.anotherAppResources,
      ),
      dependenciesSources = listOf(
        SampleBSPProjectToImport.appDependenciesSources,
        SampleBSPProjectToImport.libA2DependenciesSources,
        SampleBSPProjectToImport.libBBDependenciesSources,
        SampleBSPProjectToImport.libBB2DependenciesSources,
        SampleBSPProjectToImport.anotherAppDependenciesSources,
        SampleBSPProjectToImport.libA1DependenciesSources,
        SampleBSPProjectToImport.libBDependenciesSources,
      ),
    )

    magicMetaModel = MagicMetaModel.create(magicMetaModelProjectConfig, projectDetails)
  }

  companion object {
    fun getInstance(project: Project): MagicMetaModelService =
      project.getService(MagicMetaModelService::class.java)
  }
}
