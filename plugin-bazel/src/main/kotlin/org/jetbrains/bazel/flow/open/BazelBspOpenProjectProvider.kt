package org.jetbrains.bazel.flow.open

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.importing.AbstractOpenProjectProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.coroutines.CoroutineService

internal class BazelBspOpenProjectProvider : AbstractOpenProjectProvider() {
  private val log = logger<BazelBspOpenProjectProvider>()

  override val systemId = BazelPluginConstants.SYSTEM_ID

  override fun isProjectFile(file: VirtualFile): Boolean =
    file.isFile && file.name in BazelPluginConstants.WORKSPACE_FILE_NAMES

  @Deprecated("it is still here because it is still being used by the platform")
  override fun linkToExistingProject(projectFile: VirtualFile, project: Project) {
    CoroutineService.getInstance(project).start {
      linkToExistingProjectAsync(projectFile, project)
    }
  }

  override suspend fun linkToExistingProjectAsync(projectFile: VirtualFile, project: Project) {
    log.debug("Link BazelBsp project $projectFile to existing project ${project.name}")
    performOpenBazelProjectViaBspPlugin(project, projectFile)
  }
}
