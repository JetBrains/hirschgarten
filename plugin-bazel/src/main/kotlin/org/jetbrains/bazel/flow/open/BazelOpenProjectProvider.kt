package org.jetbrains.bazel.flow.open

import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.importing.AbstractOpenProjectProvider
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelPluginConstants

private val log = logger<BazelOpenProjectProvider>()

internal class BazelOpenProjectProvider : AbstractOpenProjectProvider() {
  override val systemId: ProjectSystemId
    get() = BazelPluginConstants.SYSTEM_ID

  // intentionally overriding the visibility to `public` from `protected` in [AbstractOpenProjectProvider]
  // should work vice versa: ExternalSystemUnlinkedProjectAware.EP_NAME.findFirstSafe { it.systemId == systemId }
  public override fun isProjectFile(file: VirtualFile): Boolean =
    file.isFile && file.name in (Constants.WORKSPACE_FILE_NAMES)

  public override suspend fun linkProject(projectFile: VirtualFile, project: Project) {
    log.debug { "Link BazelBsp project $projectFile to existing project ${project.name}" }
    // todo it is incorrect
    project.initProperties(projectFile)
    configureProjectCounterPlatform(project)
  }
}
