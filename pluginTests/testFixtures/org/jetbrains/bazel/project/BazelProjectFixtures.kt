package org.jetbrains.bazel.project

import com.intellij.configurationStore.ProjectStoreImpl
import com.intellij.configurationStore.ProjectStorePathManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.project.stateStore
import com.intellij.testFramework.refreshVfs
import com.intellij.util.io.createDirectories
import org.jetbrains.bazel.flow.open.BazelProjectStoreDescriptor
import java.nio.file.Path

object BazelProjectFixtures {
  fun initializeBazelProject(project: Project, rootDir: Path) {
    rootDir.createDirectories().refreshVfs()
    val projectStoreImpl = project.stateStore as ProjectStoreImpl
    projectStoreImpl.storeDescriptor = BazelProjectStoreDescriptor(
      projectIdentityFile = rootDir.resolve("MODULE.bazel"),
      dotIdea = rootDir.resolve(Project.DIRECTORY_STORE_FOLDER),
      historicalProjectBasePath = rootDir,
      workspaceXml = rootDir.resolve("workspace.xml")
    )
  }

  fun initializeBazelProject(project: Project, rootDir: String) {
    initializeBazelProject(project, rootDir.toNioPathOrNull()!!)
  }

  fun deinitializeBazelProject(project: Project) {
    val projectStoreImpl = project.stateStore as ProjectStoreImpl
    val basePath = projectStoreImpl.storeDescriptor.historicalProjectBasePath
    projectStoreImpl.storeDescriptor = ProjectStorePathManager.getInstance().getStoreDescriptor(basePath)
  }
}
