package org.jetbrains.bazel.android

import com.android.tools.idea.model.AndroidModel
import com.android.tools.idea.projectsystem.getProjectSystem
import com.intellij.facet.ProjectFacetManager
import com.intellij.openapi.application.readActionBlocking
import com.intellij.openapi.project.Project
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.bazel.config.BspFeatureFlags
import org.jetbrains.bazel.coroutines.BspCoroutineService
import org.jetbrains.bazel.startup.BspProjectActivity
import org.jetbrains.bazel.target.moduleEntity
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.workspacemodel.entities.androidAddendumEntity

class BspAndroidModelUpdater : BspProjectActivity() {
  override suspend fun Project.executeForBspProject() {
    targetUtils.registerSyncListener {
      if (!BspFeatureFlags.isAndroidSupportEnabled) return@registerSyncListener
      BspCoroutineService.getInstance(this).start {
        readActionBlocking {
          updateAndroidModel(this)
        }
        val syncManager = this.getProjectSystem().getSyncManager() as BspProjectSystemSyncManager
        syncManager.notifySyncEnded(this)
      }
    }
    if (!BspFeatureFlags.isAndroidSupportEnabled) return
    updateAndroidModel(this)
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
    return BspAndroidModel(androidFacet, manifestOverrides)
  }
}
