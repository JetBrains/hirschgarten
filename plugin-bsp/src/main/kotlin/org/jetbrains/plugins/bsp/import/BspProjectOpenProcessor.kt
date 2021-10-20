package org.jetbrains.plugins.bsp.import

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.projectImport.ProjectOpenProcessor
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import java.nio.file.Paths
import javax.swing.Icon

class BspProjectOpenProcessor : ProjectOpenProcessor() {

  override fun getName(): String = BspPluginBundle.message("plugin.name")

  override fun getIcon(): Icon = BspPluginIcons.bsp

  // TODO
  override fun canOpenProject(file: VirtualFile): Boolean = true

  override fun doOpenProject(
    virtualFile: VirtualFile,
    projectToClose: Project?,
    forceOpenInNewFrame: Boolean
  ): Project? {
    // TODO better options
    val options = OpenProjectTask(isNewProject = true)
    val project = ProjectManagerEx.getInstanceEx().openProject(Paths.get(virtualFile.path), options)!!

    val magicMetaModelService = MagicMetaModelService.getInstance(project)
    val magicMetaModel = magicMetaModelService.magicMetaModel

    runWriteAction { magicMetaModel.loadDefaultTargets() }

    return project
  }
}
