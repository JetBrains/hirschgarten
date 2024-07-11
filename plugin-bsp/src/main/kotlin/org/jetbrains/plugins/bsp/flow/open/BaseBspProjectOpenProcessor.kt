package org.jetbrains.plugins.bsp.flow.open

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtilCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.projectImport.ProjectOpenProcessor
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import java.nio.file.Path

private val log = logger<BaseBspProjectOpenProcessor>()

public abstract class BaseBspProjectOpenProcessor(private val buildToolId: BuildToolId) : ProjectOpenProcessor() {
  override fun doOpenProject(
    virtualFile: VirtualFile,
    projectToClose: Project?,
    forceOpenInNewFrame: Boolean,
  ): Project? {
    log.info("Opening project :$virtualFile with build tool id: $buildToolId")
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
  thisLogger().debug("Initializing properties for project: $projectRootDir and build tool id: $buildToolId")

  this.isBspProject = true
  this.rootDir = projectRootDir
  this.buildToolId = buildToolId
}
