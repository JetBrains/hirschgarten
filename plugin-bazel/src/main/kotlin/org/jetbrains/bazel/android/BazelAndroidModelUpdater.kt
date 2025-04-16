package org.jetbrains.bazel.android

import com.android.tools.idea.model.AndroidModel
import com.android.tools.idea.projectsystem.getProjectSystem
import com.intellij.facet.ProjectFacetManager
import com.intellij.openapi.application.readActionBlocking
import com.intellij.openapi.project.Project
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.bazel.target.moduleEntity
import org.jetbrains.bazel.workspacemodel.entities.androidAddendumEntity

class BazelAndroidModelUpdater : ProjectSyncHook {
  override fun isEnabled(project: Project): Boolean = BazelFeatureFlags.isAndroidSupportEnabled

  override suspend fun onSync(environment: ProjectSyncHookEnvironment) {
    val project = environment.project
    BazelCoroutineService.getInstance(project).start {
      readActionBlocking {
        updateAndroidModel(project)
      }
      val syncManager = project.getProjectSystem().getSyncManager() as BazelProjectSystemSyncManager
      syncManager.notifySyncEnded(project)
    }
  }

  private fun updateAndroidModel(project: Project) {
    val androidFacets = ProjectFacetManager.getInstance(project).getFacets(AndroidFacet.ID)
    for (androidFacet in androidFacets) {
      AndroidModel.set(androidFacet, getAndroidModel(androidFacet))
    }
  }

  private fun getAndroidModel(androidFacet: AndroidFacet): AndroidModel? {
    val module = androidFacet.module
    val androidAddendum = module.moduleEntity?.androidAddendumEntity ?: return null
    val manifestOverrides = androidAddendum.manifestOverrides
    return BazelAndroidModel(androidFacet, manifestOverrides)
  }
}
