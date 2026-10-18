package org.jetbrains.bazel.project

import com.intellij.configurationStore.ProjectStoreImpl
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.refreshAndFindVirtualFile
import com.intellij.project.stateStore
import com.intellij.testFramework.refreshVfs
import com.intellij.util.io.createDirectories
import org.jetbrains.bazel.flow.open.BazelProjectStoreDescriptor
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.languages.projectview.project.ProjectViewFileLocalizer.pickProjectViewFileForProject
import org.jetbrains.bazel.startup.setBazelStartupSyncEnabledInTests
import java.nio.file.Path
import kotlin.io.path.isRegularFile

object BazelProjectFixtures {
  /**
   * Marks a test project as Bazel. Set [runStartupSync] to test automatic startup sync.
   * Without [projectViewFile], the plugin picks the project view as for a real project.
   */
  fun initializeBazelProject(project: Project, rootDir: Path, runStartupSync: Boolean = false, projectViewFile: Path? = null) {
    rootDir.createDirectories().refreshVfs()
    val projectStoreImpl = project.stateStore as ProjectStoreImpl
    val projectIdentityFile = rootDir.resolve("MODULE.bazel")
    setBazelStartupSyncEnabledInTests(project, runStartupSync)
    projectStoreImpl.storeDescriptor = BazelProjectStoreDescriptor(
      projectIdentityFile = projectIdentityFile,
      dotIdea = rootDir.resolve(Project.DIRECTORY_STORE_FOLDER),
      historicalProjectBasePath = rootDir,
      projectViewFile = projectViewFile ?: pickProjectViewFileForProject(projectIdentityFile, rootDir),
    )
    // projectview virtual file lookup requires a VFS refresh in test environment
    ProjectViewService.getInstance(project).projectViewPath?.refreshAndFindVirtualFile()
  }

  fun initializeBazelProject(project: Project, rootDir: String, runStartupSync: Boolean = false) {
    val rootDirPath = rootDir.toNioPathOrNull() ?: error("NIO path not found for $rootDir")
    initializeBazelProject(project, rootDirPath, runStartupSync)
  }

  /** Marks a test project as Bazel through a project view. Set [runStartupSync] to test automatic startup sync. */
  fun initializeBazelProjectViaProjectView(project: Project, projectViewFile: Path, runStartupSync: Boolean = false) {
    require(projectViewFile.isRegularFile()) { "Project view file must be a regular file" }
    val rootDir = projectViewFile.parent
    val projectStoreImpl = project.stateStore as ProjectStoreImpl
    setBazelStartupSyncEnabledInTests(project, runStartupSync)
    projectStoreImpl.storeDescriptor = BazelProjectStoreDescriptor(
      projectIdentityFile = projectViewFile,
      dotIdea = rootDir.resolve(Project.DIRECTORY_STORE_FOLDER),
      historicalProjectBasePath = rootDir,
      projectViewFile = projectViewFile,
    )
  }

  fun restoreProjectStoreOnDispose(project: Project, disposable: Disposable) {
    val projectStoreImpl = project.stateStore as ProjectStoreImpl
    val storeDescriptor = projectStoreImpl.storeDescriptor
    Disposer.register(disposable) {
      projectStoreImpl.storeDescriptor = storeDescriptor
      setBazelStartupSyncEnabledInTests(project, true)
    }
  }
}
