package org.jetbrains.plugins.bsp.flow.open

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.PlatformProjectOpenProcessor
import com.intellij.projectImport.ProjectOpenProcessor
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.config.BspProjectProperties
import org.jetbrains.plugins.bsp.config.BspProjectPropertiesService
import org.jetbrains.plugins.bsp.config.ProjectProperties
import org.jetbrains.plugins.bsp.config.ProjectPropertiesService
import org.jetbrains.plugins.bsp.extension.points.BspConnectionDetailsGeneratorExtension
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionDetailsGeneratorProvider
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionFilesProvider
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
  ): Project? =
    PlatformProjectOpenProcessor.getInstance().doOpenProject(virtualFile, projectToClose, forceOpenInNewFrame)
      ?.also { initServices(it, virtualFile) }

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
