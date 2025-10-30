package org.jetbrains.bazel.flow.open

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.projectImport.ProjectOpenProcessor
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.settings.bazel.openProjectViewInEditor
import org.jetbrains.bazel.settings.bazel.setProjectViewPath
import java.nio.file.Path
import javax.swing.Icon

private val log = logger<BazelProjectOpenProcessor>()

val BUILD_FILE_GLOB = "{${Constants.BUILD_FILE_NAMES.joinToString(",")}}"

/**
 * Refrain from using [VirtualFile.getChildren] as it causes performance issues in large projects, such as [BAZEL-1717](https://youtrack.jetbrains.com/issue/BAZEL-1717)
 */
internal class BazelProjectOpenProcessor : ProjectOpenProcessor() {
  override fun doOpenProject(
    virtualFile: VirtualFile,
    projectToClose: Project?,
    forceOpenInNewFrame: Boolean,
  ): Project? {
    log.info("Opening project :$virtualFile")
    val (projectStoreBaseDir, options) =
      calculateOpenProjectTask(virtualFile, forceOpenInNewFrame, projectToClose)

    return ProjectManagerEx
      .getInstanceEx()
      .openProject(projectStoreBaseDir, options)
  }

  override suspend fun openProjectAsync(
    virtualFile: VirtualFile,
    projectToClose: Project?,
    forceOpenInNewFrame: Boolean,
  ): Project? {
    log.info("Opening asynchronously project :$virtualFile")
    val (projectStoreBaseDir, options) =
      calculateOpenProjectTask(virtualFile, forceOpenInNewFrame, projectToClose)

    return ProjectManagerEx
      .getInstanceEx()
      .openProjectAsync(projectStoreBaseDir, options)
  }

  private fun calculateOpenProjectTask(
    virtualFile: VirtualFile,
    forceOpenInNewFrame: Boolean,
    projectToClose: Project?,
  ): Pair<Path, OpenProjectTask> {
    // todo why do we even need to calculate the project root dir?
    // todo refactor
    val projectRootDir = findProjectFolderFromVFile(virtualFile)!!
    val projectViewPath = getProjectViewPath(projectRootDir, virtualFile)
    val projectStoreBaseDir = projectViewPath ?: virtualFile.toNioPath()

    return projectStoreBaseDir to
      OpenProjectTask {
        runConfigurators = true
        isRefreshVfsNeeded = !ApplicationManager.getApplication().isUnitTestMode

        this.projectRootDir = projectRootDir.toNioPathOrNull()
        this.forceOpenInNewFrame = forceOpenInNewFrame
        this.projectToClose = projectToClose
        this.createModule = Registry.`is`("bazel.create.fake.module.on.project.import")

        beforeOpen = { project ->
          project.initProperties(projectRootDir)

          if (projectViewPath != null) {
            project.setProjectViewPath(projectViewPath)
            openProjectViewInEditor(project, projectViewPath)
          }

          true
        }
      }
  }

  override val icon: Icon
    get() = BazelPluginIcons.bazel

  override val name: String
    get() = BazelPluginConstants.BAZEL_DISPLAY_NAME

  override val isStrongProjectInfoHolder: Boolean
    get() = BazelFeatureFlags.autoOpenProjectIfPresent

  /**
   * The project is only eligible to be opened with Bazel Plugin if workspace files can be reached from the given vFile
   */
  override fun canOpenProject(file: VirtualFile): Boolean {
    if (file.isDirectory && file.findChild(Project.DIRECTORY_STORE_FOLDER) != null) return false

    return findProjectFolderFromVFile(file) != null
  }
}
