package org.jetbrains.plugins.bsp.startup

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.projectImport.ProjectOpenProcessor
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
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

    val magicMetaModelService = MagicMetaModelService.getInstance(project)
    val magicMetaModel = magicMetaModelService.magicMetaModel

    runWriteAction {
      magicMetaModel.loadDefaultTargets()
    }

    return project
  }
}
