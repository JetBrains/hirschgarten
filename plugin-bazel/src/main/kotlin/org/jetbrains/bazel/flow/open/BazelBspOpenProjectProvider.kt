package org.jetbrains.bazel.flow.open

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.importing.AbstractOpenProjectProvider
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import org.jetbrains.bazel.config.BazelPluginConstants

internal class BazelBspOpenProjectProvider : AbstractOpenProjectProvider() {
  private val log = logger<BazelBspOpenProjectProvider>()

  override val systemId: ProjectSystemId = BazelPluginConstants.SYSTEM_ID

  public override fun isProjectFile(file: VirtualFile): Boolean = file.isFile && file.name in BazelPluginConstants.WORKSPACE_FILE_NAMES

  public override suspend fun linkProject(projectFile: VirtualFile, project: Project) {
    log.debug("Link BazelBsp project $projectFile to existing project ${project.name}")
    performOpenBazelProjectViaBspPlugin(project, projectFile)
  }
}
