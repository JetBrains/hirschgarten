package org.jetbrains.plugins.bsp.flow.open

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.DirectoryProjectConfigurator
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.impl.internal
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.services.BspCoroutineService

/**
 * Clean up any modules showing up due to the platform hack
 * https://youtrack.jetbrains.com/issue/IDEA-321160/Platform-solution-for-the-initial-state-of-the-project-model-on-the-first-open
 */
internal class CounterPlatformProjectConfigurator : DirectoryProjectConfigurator {
  override fun configureProject(
    project: Project,
    baseDir: VirtualFile,
    moduleRef: Ref<Module>,
    isProjectCreatedWithWizard: Boolean,
  ) {
    if (!project.isBspProject) return

    val workspaceModel = WorkspaceModel.getInstance(project)
    val fakeModules =
      workspaceModel.internal.entityStorage.current.entities(ModuleEntity::class.java)

    BspCoroutineService.getInstance(project).start {
      writeAction {
        workspaceModel.updateProjectModel("Counter platform fake modules") {
          fakeModules.forEach { fakeModule -> it.removeEntity(fakeModule) }
        }
      }
    }
  }
}
