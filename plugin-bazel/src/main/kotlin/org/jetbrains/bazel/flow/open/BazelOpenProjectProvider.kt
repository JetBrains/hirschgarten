package org.jetbrains.bazel.flow.open

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.importing.AbstractOpenProjectProvider
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.startup.BazelProjectActivity
import org.jetbrains.bazel.ui.widgets.tool.window.all.targets.registerBazelToolWindow

private val log = logger<BazelOpenProjectProvider>()

internal class BazelOpenProjectProvider : AbstractOpenProjectProvider() {
  override val systemId: ProjectSystemId
    get() = BazelPluginConstants.SYSTEM_ID

  // intentionally overriding the visibility to `public` from `protected` in [AbstractOpenProjectProvider]
  @Suppress("RedundantVisibilityModifier")
  public override fun isProjectFile(file: VirtualFile): Boolean = file.isFile && file.name in Constants.WORKSPACE_FILE_NAMES

  @Suppress("RedundantVisibilityModifier")
  public override suspend fun linkProject(projectFile: VirtualFile, project: Project) {
    log.debug("Link BazelBsp project $projectFile to existing project ${project.name}")
    performOpenBazelProject(project, projectFile)
  }
}

internal suspend fun performOpenBazelProject(project: Project?, projectRootDir: VirtualFile?) {
  if (projectRootDir != null && project != null) {
    project.initProperties(projectRootDir)
    registerBazelToolWindow(project)
    configureProjectCounterPlatform(project)
    BazelCoroutineService.getInstance(project).start {
      StartupActivity.POST_STARTUP_ACTIVITY.extensionList.filterIsInstance<BazelProjectActivity>().forEach {
        it.execute(project)
      }
    }
  }
}
