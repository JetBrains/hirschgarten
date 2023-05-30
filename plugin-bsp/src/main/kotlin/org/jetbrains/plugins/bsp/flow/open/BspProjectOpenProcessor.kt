package org.jetbrains.plugins.bsp.flow.open

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtilCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.projectImport.ProjectOpenProcessor
import com.intellij.projectImport.ProjectOpenedCallback
import java.nio.file.Path
import javax.swing.Icon
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.config.BspProjectProperties
import org.jetbrains.plugins.bsp.config.BspProjectPropertiesService
import org.jetbrains.plugins.bsp.config.ProjectProperties
import org.jetbrains.plugins.bsp.config.ProjectPropertiesService
import org.jetbrains.plugins.bsp.extension.points.BspConnectionDetailsGeneratorExtension
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionDetailsGeneratorProvider
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionFilesProvider
import org.jetbrains.plugins.bsp.services.MagicMetaModelService

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

    beforeOpen = { initServices(it, virtualFile); true }
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
      )
    )
    ApplicationManager.getApplication().invokeLater {
      runWriteAction {
        magicMetaModelService.value.loadDefaultTargets().applyOnWorkspaceModel()
      }
    }
  }

  private fun initServices(project: Project, projectRootDir: VirtualFile) {
    initBspProjectPropertiesService(project)
    initProjectPropertiesService(project, projectRootDir)
  }

  private fun initBspProjectPropertiesService(project: Project) {
    val bspProjectPropertiesService = BspProjectPropertiesService.getInstance(project)

    val bspProjectProperties = BspProjectProperties(
      isBspProject = true,
    )
    bspProjectPropertiesService.init(bspProjectProperties)
  }

  private fun initProjectPropertiesService(project: Project, projectRootDir: VirtualFile) {
    val projectPropertiesService = ProjectPropertiesService.getInstance(project)

    val projectProperties = ProjectProperties(
      projectRootDir = projectRootDir,
    )
    projectPropertiesService.init(projectProperties)
  }
}
