package org.jetbrains.bazel.flow.open

import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.InitProjectActivity
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.project.ProjectStoreOwner
import org.jetbrains.bazel.config.BazelProjectProperties
import org.jetbrains.bazel.settings.bazel.setProjectViewPath

private class OpenBazelProjectAndSyncStartupActivity : InitProjectActivity {
  override suspend fun run(project: Project) {
    if (project !is ProjectStoreOwner) return

    val storeDescriptor = project.componentStore
      .storeDescriptor
    if (storeDescriptor !is BazelProjectStoreDescriptor) return

    val virtualFile = storeDescriptor
      .projectIdentityFile
      .let(LocalFileSystem.getInstance()::refreshAndFindFileByNioFile)

    if (virtualFile == null) {
      logger<OpenBazelProjectAndSyncStartupActivity>().error("Project identity file not found")
      return
    }

    // todo remove rootDir!
    // todo check workspace model emptiness
    val projectRootDir = project.serviceAsync<BazelProjectProperties>()
      .rootDir
      ?: findProjectFolderFromVFile(virtualFile)!!

    val projectViewPath = getProjectViewPath(projectRootDir, virtualFile)
    if (projectViewPath != null) {
      project.setProjectViewPath(projectViewPath)
    }

    project.initProperties(projectRootDir)
  }
}
