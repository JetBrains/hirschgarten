package org.jetbrains.plugins.bsp.flow.open

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtilCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.projectImport.ProjectOpenProcessor
import com.intellij.projectImport.ProjectOpenedCallback
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.config.projectRootDir
import org.jetbrains.plugins.bsp.extension.points.BspConnectionDetailsGeneratorExtension
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionDetailsGeneratorProvider
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionFilesProvider
import org.jetbrains.plugins.bsp.services.BspCoroutineService
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import java.nio.file.Path
import javax.swing.Icon

public class BspProjectOpenProcessor : ProjectOpenProcessor() {

  override val name: String = BspPluginBundle.message("plugin.name")

  override val icon: Icon = BspPluginIcons.bsp

  override fun canOpenProject(file: VirtualFile): Boolean {
    val bspConnectionFilesProvider = BspConnectionFilesProvider(file)
    val bspConnectionDetailsGeneratorProvider =
      BspConnectionDetailsGeneratorProvider(file, BspConnectionDetailsGeneratorExtension.extensions())

    return bspConnectionFilesProvider.isAnyBspConnectionFileDefined() or
      bspConnectionDetailsGeneratorProvider.canGenerateAnyBspConnectionDetailsFile()
  }

  override fun doOpenProject(
    virtualFile: VirtualFile,
    projectToClose: Project?,
    forceOpenInNewFrame: Boolean
  ): Project? {
    val projectPath = virtualFile.toNioPath()
    val openProjectTask = calculateOpenProjectTask(projectPath, forceOpenInNewFrame, projectToClose, virtualFile)

    return ProjectManagerEx.getInstanceEx().openProject(projectPath, openProjectTask)
  }

  private fun calculateOpenProjectTask(
    projectPath: Path,
    forceOpenInNewFrame: Boolean,
    projectToClose: Project?,
    virtualFile: VirtualFile
  ) = OpenProjectTask {
    runConfigurators = true
    isNewProject = !ProjectUtilCore.isValidProjectPath(projectPath)
    isRefreshVfsNeeded = !ApplicationManager.getApplication().isUnitTestMode

    this.forceOpenInNewFrame = forceOpenInNewFrame
    this.projectToClose = projectToClose

    beforeOpen = { initProperties(it, virtualFile); true }
    callback = ProjectOpenedCallback { project, _ -> initializeEmptyMagicMetaModel(project) }
  }

  private fun initializeEmptyMagicMetaModel(project: Project) {
    val magicMetaModelService = MagicMetaModelService.getInstance(project)
    magicMetaModelService.initializeMagicModel(
      ProjectDetails(
        targetsId = emptyList(),
        targets = emptySet(),
        sources = emptyList(),
        resources = emptyList(),
        dependenciesSources = emptyList(),
        javacOptions = emptyList(),
        outputPathUris = emptyList(),
      )
    )

    BspCoroutineService.getInstance().start {
      magicMetaModelService.value.loadDefaultTargets().applyOnWorkspaceModel()
    }
  }

  private fun initProperties(project: Project, projectRootDir: VirtualFile) {
    project.isBspProject = true
    project.projectRootDir = projectRootDir
  }
}
