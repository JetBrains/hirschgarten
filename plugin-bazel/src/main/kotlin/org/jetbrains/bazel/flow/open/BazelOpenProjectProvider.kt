package org.jetbrains.bazel.flow.open

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.importing.AbstractOpenProjectProvider
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.startup.BazelStartupActivity

internal class BazelOpenProjectProvider : AbstractOpenProjectProvider() {
  private val log = logger<BazelOpenProjectProvider>()

  override val systemId: ProjectSystemId = BazelPluginConstants.SYSTEM_ID

  // intentionally overriding the visibility to `public` from `protected` in [AbstractOpenProjectProvider]
  @Suppress("RedundantVisibilityModifier")
  public override fun isProjectFile(file: VirtualFile): Boolean = file.isFile && file.name in Constants.WORKSPACE_FILE_NAMES

  @Suppress("RedundantVisibilityModifier")
  public override suspend fun linkProject(projectFile: VirtualFile, project: Project) {
    log.debug("Link BazelBsp project $projectFile to existing project ${project.name}")
    performOpenBazelProject(project, projectFile)
  }
}

internal fun performOpenBazelProject(project: Project?, projectRootDir: VirtualFile?) {
  if (projectRootDir != null && project != null) {
    project.initProperties(projectRootDir)
    CounterPlatformProjectConfigurator().configureProject(project)
    BazelCoroutineService.getInstance(project).start {
      BazelStartupActivity().execute(project)
    }
  }
}
