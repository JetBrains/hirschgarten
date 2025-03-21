package org.jetbrains.bazel.flow.open

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtilCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.projectImport.ProjectOpenProcessor
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import java.nio.file.Path

private val log = logger<BaseProjectOpenProcessor>()

abstract class BaseProjectOpenProcessor : ProjectOpenProcessor() {
  override fun doOpenProject(
    virtualFile: VirtualFile,
    projectToClose: Project?,
    forceOpenInNewFrame: Boolean,
  ): Project? {
    log.info("Opening project :$virtualFile")
    val vFileToOpen = calculateProjectFolderToOpen(virtualFile)
    val projectPath = vFileToOpen.toNioPath()
    val openProjectTask =
      calculateOpenProjectTask(
        projectPath = projectPath,
        forceOpenInNewFrame = forceOpenInNewFrame,
        projectToClose = projectToClose,
        vFileToOpen = vFileToOpen,
        originalVFile = virtualFile,
      )

    return ProjectManagerEx.getInstanceEx().openProject(projectPath, openProjectTask)
  }

  private fun calculateOpenProjectTask(
    projectPath: Path,
    forceOpenInNewFrame: Boolean,
    projectToClose: Project?,
    vFileToOpen: VirtualFile,
    originalVFile: VirtualFile,
  ): OpenProjectTask =
    OpenProjectTask {
      runConfigurators = true
      isNewProject = !ProjectUtilCore.isValidProjectPath(projectPath)
      isRefreshVfsNeeded = !ApplicationManager.getApplication().isUnitTestMode

      this.forceOpenInNewFrame = forceOpenInNewFrame
      this.projectToClose = projectToClose

      beforeOpen = {
        it.initProperties(vFileToOpen)
        calculateBeforeOpenCallback(originalVFile).invoke(it)
        true
      }
    }

  /**
   * when a file/subdirectory is selected for opening a BSP project,
   * this method provides information about the real project directory to open
   */
  abstract fun calculateProjectFolderToOpen(virtualFile: VirtualFile): VirtualFile

  /**
   * this method can be used to set up additional project properties before opening the BSP project
   * @param originalVFile the virtual file passed to the project open processor
   */
  open fun calculateBeforeOpenCallback(originalVFile: VirtualFile): (Project) -> Unit = {}
}

fun Project.initProperties(projectRootDir: VirtualFile) {
  thisLogger().debug("Initializing properties for project: $projectRootDir")

  this.isBazelProject = true
  this.rootDir = projectRootDir
}
