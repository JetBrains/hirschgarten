package org.jetbrains.bazel.flow.open

import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.InitProjectActivity
import com.intellij.openapi.vfs.refreshAndFindVirtualFile
import com.intellij.project.ProjectStoreOwner
import org.jetbrains.bazel.config.BazelProjectProperties
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings

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
    project.initProperties(projectRootDir)

    val projectViewPath = getProjectViewPath(projectRootDir, path)
                            ?.refreshAndFindVirtualFile()
                          ?: return

    project.bazelProjectSettings = project.bazelProjectSettings
      .withNewProjectViewPath(projectViewPath)
  }
}
