package org.jetbrains.bazel.project

import com.intellij.configurationStore.ProjectStoreImpl
import com.intellij.configurationStore.ProjectStorePathManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.project.stateStore
import com.intellij.testFramework.refreshVfs
import com.intellij.util.io.createDirectories
import org.jetbrains.bazel.flow.open.BazelProjectStoreDescriptor
import org.jetbrains.bazel.languages.projectview.project.ProjectViewFileLocalizer.pickProjectViewFileForProject
import java.nio.file.Path
import kotlin.io.path.isRegularFile

object BazelProjectFixtures {
  fun initializeBazelProject(project: Project, rootDir: Path) {
    rootDir.createDirectories().refreshVfs()
    val projectStoreImpl = project.stateStore as ProjectStoreImpl
    val projectIdentityFile = rootDir.resolve("MODULE.bazel")
    projectStoreImpl.storeDescriptor = BazelProjectStoreDescriptor(
      projectIdentityFile = projectIdentityFile,
      dotIdea = rootDir.resolve(Project.DIRECTORY_STORE_FOLDER),
      historicalProjectBasePath = rootDir,
      projectViewFile = pickProjectViewFileForProject(projectIdentityFile, rootDir),
    )
  }

  fun initializeBazelProject(project: Project, rootDir: String) {
    initializeBazelProject(project, rootDir.toNioPathOrNull()!!)
  }

  fun initializeBazelProjectViaProjectView(project: Project, projectViewFile: Path) {
    require(projectViewFile.isRegularFile()) { "Project view file must be a regular file" }
    val rootDir = projectViewFile.parent
    val projectStoreImpl = project.stateStore as ProjectStoreImpl
    projectStoreImpl.storeDescriptor = BazelProjectStoreDescriptor(
      projectIdentityFile = projectViewFile,
      dotIdea = rootDir.resolve(Project.DIRECTORY_STORE_FOLDER),
      historicalProjectBasePath = rootDir,
      projectViewFile = projectViewFile,
    )
  }

  fun deinitializeBazelProject(project: Project) {
    val projectStoreImpl = project.stateStore as ProjectStoreImpl
    val basePath = projectStoreImpl.storeDescriptor.historicalProjectBasePath
    projectStoreImpl.storeDescriptor = ProjectStorePathManager.getInstance().getStoreDescriptor(basePath)
  }
}
