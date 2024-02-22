package org.jetbrains.plugins.bsp.flow.open

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtilCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.projectImport.ProjectOpenProcessor
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import java.nio.file.Path

public abstract class BaseBspProjectOpenProcessor(private val buildToolId: BuildToolId) : ProjectOpenProcessor() {
  override fun doOpenProject(
    virtualFile: VirtualFile,
    projectToClose: Project?,
    forceOpenInNewFrame: Boolean,
  ): Project? {
    val projectPath = virtualFile.toNioPath()
    val openProjectTask = calculateOpenProjectTask(projectPath, forceOpenInNewFrame, projectToClose, virtualFile)

    return ProjectManagerEx.getInstanceEx().openProject(projectPath, openProjectTask)
  }

  internal fun calculateOpenProjectTask(
    projectPath: Path,
    forceOpenInNewFrame: Boolean,
    projectToClose: Project?,
    virtualFile: VirtualFile,
  ): OpenProjectTask = OpenProjectTask {
    runConfigurators = true
    isNewProject = !ProjectUtilCore.isValidProjectPath(projectPath)
    isRefreshVfsNeeded = !ApplicationManager.getApplication().isUnitTestMode

    this.forceOpenInNewFrame = forceOpenInNewFrame
    this.projectToClose = projectToClose

    beforeOpen = { it.initProperties(virtualFile, buildToolId); true }
  }
}

public fun Project.initProperties(projectRootDir: VirtualFile, buildToolId: BuildToolId) {
  this.isBspProject = true
  this.rootDir = projectRootDir
  this.buildToolId = buildToolId
}
