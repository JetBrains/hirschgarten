package org.jetbrains.bazel.projectAware

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectAware
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectId
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectListener
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectReloadContext
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTracker
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemRefreshStatus
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.bazelProjectProperties
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.status.SyncStatusListener
import org.jetbrains.bazel.sync.task.ProjectSyncTask

abstract class BazelProjectAware(private val workspace: BazelWorkspace) : ExternalSystemProjectAware {
  override val settingsFiles: Set<String>
    get() = runBlocking { calculateBazelConfigFiles(workspace.project) }

  override fun reloadProject(context: ExternalSystemProjectReloadContext) {
    if (context.isExplicitReload) {
      BazelCoroutineService.getInstance(workspace.project).start {
        ProjectSyncTask(workspace.project).sync(syncScope = SecondPhaseSync, buildProject = false)
      }
    }
  }

  override fun subscribe(listener: ExternalSystemProjectListener, parentDisposable: Disposable) {
    workspace.project.messageBus.connect().subscribe(
      SyncStatusListener.TOPIC,
      object : SyncStatusListener {
        override fun syncStarted() {
          listener.onProjectReloadStart()
        }

        override fun syncFinished(canceled: Boolean) {
          ProjectViewUtil.expandTopLevel(workspace.project)
          listener.onProjectReloadFinish(
            if (canceled) {
              ExternalSystemRefreshStatus.CANCEL
            } else {
              ExternalSystemRefreshStatus.SUCCESS
            },
          )
        }
      },
    )
  }

  companion object {
    @JvmStatic
    fun initialize(workspace: BazelWorkspace) {
      val projectAwareExtension = ProjectAwareExtension.ep.extensionList.firstOrNull()
      projectAwareExtension?.also {
        val project = workspace.project
        val projectAware =
          object : BazelProjectAware(workspace) {
            override val projectId: ExternalSystemProjectId
              get() = it.getProjectId(project.rootDir)
          }
        val projectTracker = ExternalSystemProjectTracker.getInstance(project)
        projectTracker.register(projectAware)
        projectTracker.activate(projectAware.projectId)
      }
    }

    /**
     * Shows the "Sync Bazel changes" button.
     * After using this function, the button will not disappear automatically until the project is reloaded.
     */
    @JvmStatic
    fun notify(project: Project) {
      val projectAwareExtension = ProjectAwareExtension.ep.extensionList.firstOrNull()
      projectAwareExtension?.also {
        val projectTracker = ExternalSystemProjectTracker.getInstance(project)
        projectTracker.markDirty(it.getProjectId(project.rootDir))
        projectTracker.scheduleChangeProcessing()
      }
    }
  }
}

private suspend fun calculateBazelConfigFiles(project: Project): Set<String> {
  val rootDir = project.bazelProjectProperties.rootDir
  val searchScope = GlobalSearchScope.projectScope(project)
  val projectView = project.bazelProjectSettings.projectViewPath
  val globalFiles = readAction { GLOBAL_CONFIG_FILES.map { rootDir?.findChild(it) } }
  val localFiles = readAction { LOCAL_CONFIG_FILES.flatMap { FilenameIndex.getVirtualFilesByName(it, searchScope) } }
  return (globalFiles + localFiles + listOf(projectView)).mapNotNull { it?.path }.toSet()
}

// Bazel config files to look for only in the project root
private val GLOBAL_CONFIG_FILES: Array<String> = Constants.WORKSPACE_FILE_NAMES + Constants.BAZELISK_FILE_NAMES + Constants.BAZEL_RC_FILE_NAME

// Bazel config files to look for in all project directories
private val LOCAL_CONFIG_FILES: Array<String> = Constants.BUILD_FILE_NAMES
