package org.jetbrains.bazel.flow.open

import com.intellij.ide.impl.toOpenProjectTask
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.refreshAndFindVirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.projectImport.ProjectOpenProcessor
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.languages.projectview.project.ProjectViewFileLocalizer.pickProjectViewFileForProject
import org.jetbrains.bazel.target.ModuleTargetService
import org.jetbrains.bazel.target.TargetUtils
import javax.swing.Icon

/**
 * Refrain from using [VirtualFile.getChildren] as it causes performance issues in large projects, such as [BAZEL-1717](https://youtrack.jetbrains.com/issue/BAZEL-1717)
 */
internal class BazelProjectOpenProcessor : ProjectOpenProcessor() {
  override suspend fun openProjectAsync(
    virtualFile: VirtualFile,
    projectOpenOptions: ProjectOpenOptions
  ): Project? {
    val fileBeingOpen = virtualFile.toNioPathOrNull() ?: return null
    log.info("Opening asynchronously file as a Bazel project: $fileBeingOpen")

    // The user (or another part of the plugin) may try to open the project by pointing to:
    // BUILD files, MODULE.bazel, project view file, etc. so we need to do normalization here
    val projectRootDir = findProjectFolderFromFile(fileBeingOpen) ?: return null
    log.trace { "Found project root directory: $projectRootDir" }

    val projectViewFile = pickProjectViewFileForProject(fileBeingOpen, projectRootDir)
    log.trace { "Using project view file: $projectViewFile" }

    val openProjectTask = projectOpenOptions.toOpenProjectTask().copy(
      // Setting this flag to true will remove existing .idea directory.
      // We must overwrite it because ProjectUtil#openOrImportAsync sets it to true.
      isNewProject = false,
      runConfigurators = true,

      projectRootDir = projectRootDir,
      createModule = false,

      callback = { project, _ ->
        BazelCoroutineService.getInstance(project).start {
          projectViewFile
            .refreshAndFindVirtualFile()
            ?.let { projectViewPath ->
              openProjectViewInEditor(project, projectViewPath)
            }
        }

        // force async load targets cache
        project.service<TargetUtils>()
        project.service<ModuleTargetService>()
      }
    )

    return ProjectManagerEx
      .getInstanceEx()
      .openProjectAsync(projectViewFile, openProjectTask)
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
    if (!BazelFeatureFlags.autoOpenProjectIfPresent &&
        file.isDirectory &&
        file.findChild(Project.DIRECTORY_STORE_FOLDER) != null) return false

    return findProjectFolderFromVFile(file) != null
  }

  companion object {
    private val log = logger<BazelProjectOpenProcessor>()
  }
}
