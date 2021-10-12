package org.jetbrains.plugins.bsp.startup

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.project.stateStore
import com.intellij.projectImport.ProjectOpenProcessor
import com.intellij.workspaceModel.ide.WorkspaceModel
import com.intellij.workspaceModel.ide.getInstance
import com.intellij.workspaceModel.storage.url.VirtualFileUrlManager
import org.jetbrains.magicmetamodel.MagicMetaModel
import org.jetbrains.magicmetamodel.MagicMetaModelProjectConfig
import org.jetbrains.magicmetamodel.ProjectDetails
import java.nio.file.Paths

class TestStartupActivity : ProjectOpenProcessor() {

  override fun getName(): String {
    TODO("Not yet implemented")
  }

  override fun canOpenProject(file: VirtualFile): Boolean {
    return true
  }

  override fun doOpenProject(
    virtualFile: VirtualFile,
    projectToClose: Project?,
    forceOpenInNewFrame: Boolean
  ): Project? {

    val options = OpenProjectTask(isNewProject = true)
    val project = ProjectManagerEx.getInstanceEx().openProject(Paths.get(virtualFile.path), options)!!

    val workspaceModel = WorkspaceModel.getInstance(project)

    val virtualFileUrlManager = VirtualFileUrlManager.getInstance(project)
    val projectBaseDir = project.stateStore.projectBasePath

    val magicMetaModelProjectConfig = MagicMetaModelProjectConfig(workspaceModel, virtualFileUrlManager, projectBaseDir)

    val projectDetails = ProjectDetails(
      targetsId = SampleBSPProjectToImport.allTargetsIds,
      targets = listOf(
        SampleBSPProjectToImport.libATarget,
        SampleBSPProjectToImport.appTarget,
        SampleBSPProjectToImport.libBBTarget,
        SampleBSPProjectToImport.libBTarget,
      ),
      sources = listOf(
        SampleBSPProjectToImport.appSources,
        SampleBSPProjectToImport.libASources,
        SampleBSPProjectToImport.libBBSources,
        SampleBSPProjectToImport.libBSources,
      ),
      resources = listOf(
        SampleBSPProjectToImport.appResources,
        SampleBSPProjectToImport.libBBResources,
      ),
      dependenciesSources = listOf(
        SampleBSPProjectToImport.appDependenciesSources,
        SampleBSPProjectToImport.libBBDependenciesSources,
        SampleBSPProjectToImport.libADependenciesSources,
        SampleBSPProjectToImport.libBDependenciesSources,
      ),
    )

    val magicMetaModel = MagicMetaModel.create(magicMetaModelProjectConfig, projectDetails)

    runWriteAction {
      magicMetaModel.loadDefaultTargets()
    }

    return project
  }
}
