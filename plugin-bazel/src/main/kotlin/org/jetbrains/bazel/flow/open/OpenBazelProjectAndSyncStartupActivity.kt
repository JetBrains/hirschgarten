package org.jetbrains.bazel.flow.open

import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.InitProjectActivity
import com.intellij.project.ProjectStoreOwner
import org.jetbrains.bazel.config.BazelProjectProperties
import org.jetbrains.bazel.settings.bazel.setProjectViewPath

// todo rename to ImportBazelProjectAndSyncStartupActivity
internal class OpenBazelProjectAndSyncStartupActivity : InitProjectActivity {
  override suspend fun run(project: Project) {
    if (project !is ProjectStoreOwner) return

    val storeDescriptor = project.componentStore
      .storeDescriptor
    if (storeDescriptor !is BazelProjectStoreDescriptor) return

    // todo remove rootDir!
    // todo check workspace model emptiness
    if (project.serviceAsync<BazelProjectProperties>().rootDir != null) return

    // todo duplicates org.jetbrains.bazel.flow.open.BazelProjectOpenProcessor.calculateOpenProjectTask
    val path = storeDescriptor.projectIdentityFile
    val projectRootDir = findProjectFolderFromFile(path)!!

    val projectViewPath = getProjectViewPath(projectRootDir, path)
    if (projectViewPath != null) {
                          project.setProjectViewPath(projectViewPath)
    }

    project.initProperties(projectRootDir)
  }
}
