package org.jetbrains.bazel.flow.open

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.importing.AbstractOpenProjectProvider
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.bazel.coroutines.CoroutineService
import org.jetbrains.plugins.bsp.impl.flow.open.CounterPlatformProjectConfigurator
import org.jetbrains.plugins.bsp.impl.flow.open.initProperties
import org.jetbrains.plugins.bsp.startup.BspStartupActivity

internal class BazelBspOpenProjectProvider : AbstractOpenProjectProvider() {
  private val log = logger<BazelBspOpenProjectProvider>()

  override val systemId: ProjectSystemId = BazelPluginConstants.SYSTEM_ID

  // intentionally overriding the visibility to `public` from `protected` in [AbstractOpenProjectProvider]
  @Suppress("RedundantVisibilityModifier")
  public override fun isProjectFile(file: VirtualFile): Boolean = file.isFile && file.name in BazelPluginConstants.WORKSPACE_FILE_NAMES

  @Suppress("RedundantVisibilityModifier")
  public override suspend fun linkProject(projectFile: VirtualFile, project: Project) {
    log.debug("Link BazelBsp project $projectFile to existing project ${project.name}")
    performOpenBazelProjectViaBspPlugin(project, projectFile)
  }
}

internal fun performOpenBazelProjectViaBspPlugin(project: Project?, projectRootDir: VirtualFile?) {
  if (projectRootDir != null && project != null) {
    project.initProperties(projectRootDir, bazelBspBuildToolId)
    CounterPlatformProjectConfigurator().configureProject(project)
    CoroutineService.getInstance(project).start {
      BspStartupActivity().execute(project)
    }
  }
}
