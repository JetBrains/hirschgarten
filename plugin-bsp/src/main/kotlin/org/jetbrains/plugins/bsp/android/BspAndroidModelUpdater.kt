package org.jetbrains.plugins.bsp.android

import com.android.tools.idea.model.AndroidModel
import com.android.tools.idea.projectsystem.getProjectSystem
import com.intellij.facet.ProjectFacetManager
import com.intellij.openapi.application.readActionBlocking
import com.intellij.openapi.project.Project
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.plugins.bsp.coroutines.BspCoroutineService
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.util.moduleEntity
import org.jetbrains.plugins.bsp.startup.BspProjectActivity
import org.jetbrains.plugins.bsp.target.temporaryTargetUtils
import org.jetbrains.plugins.bsp.workspacemodel.entities.androidAddendumEntity

class BspAndroidModelUpdater : BspProjectActivity() {
  override suspend fun Project.executeForBspProject() {
    temporaryTargetUtils.registerSyncListener {
      BspCoroutineService.getInstance(this).start {
        readActionBlocking {
          updateAndroidModel(this)
        }
        val syncManager = this.getProjectSystem().getSyncManager() as BspProjectSystemSyncManager
        syncManager.notifySyncEnded(this)
      }
    }
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
