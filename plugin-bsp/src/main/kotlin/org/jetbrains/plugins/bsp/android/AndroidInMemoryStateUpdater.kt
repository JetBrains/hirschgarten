package org.jetbrains.plugins.bsp.android

import com.android.tools.idea.model.AndroidModel
import com.android.tools.idea.projectsystem.NamedIdeaSourceProviderBuilder
import com.android.tools.idea.projectsystem.ScopeType
import com.intellij.facet.ProjectFacetManager
import com.intellij.openapi.application.readActionBlocking
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.magicmetamodel.impl.workspacemodel.AndroidAddendum
import org.jetbrains.magicmetamodel.impl.workspacemodel.JavaModule
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.services.BspCoroutineService
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import java.net.URI

public class AndroidInMemoryStateUpdater : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (!project.isBspProject) {
      return
    }

    val magicMetaModel = MagicMetaModelService.getInstance(project).value

    magicMetaModel.registerTargetLoadListener {
      BspCoroutineService.getInstance(project).start {
        updateAndroidInMemoryState(project)
      }
    }

    updateAndroidInMemoryState(project)
  }

  private suspend fun updateAndroidInMemoryState(project: Project) {
    readActionBlocking { doUpdateAndroidInMemoryState(project) }
  }

  private fun doUpdateAndroidInMemoryState(project: Project) {
    val androidModules = ProjectFacetManager.getInstance(project).getModulesWithFacet(AndroidFacet.ID)
    for (module in androidModules) {
      updateAndroidFacetInMemoryState(module)
    }
    BspLightResourceClassService.getInstance(project).updateRClasses()
    BspProjectSystemSyncManager.onSyncEnded(project)
  }

  private fun updateAndroidFacetInMemoryState(module: Module) {
    val androidAddendum = module.androidAddendum ?: return
    val manifest = androidAddendum.manifest ?: return
    val sourceProvider = NamedIdeaSourceProviderBuilder
      .create(module.name, manifest.toString())
      .withScopeType(ScopeType.MAIN)
      .withResDirectoryUrls(androidAddendum.resourceFolders.map(URI::toString))
      .build()

    val androidModel = BspAndroidModel(sourceProvider)
    val facet = AndroidFacet.getInstance(module) ?: return
    AndroidModel.set(facet, androidModel)
  }

  private val Module.androidAddendum: AndroidAddendum?
    get() {
      val magicMetaModel = MagicMetaModelService.getInstance(this.project).value
      val moduleDetails = magicMetaModel.getDetailsForTargetId(this.name)
      if (moduleDetails !is JavaModule) return null
      return moduleDetails.androidAddendum
    }
}
