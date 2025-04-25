package org.jetbrains.bazel.flow.open

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.DirectoryProjectConfigurator
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.impl.WorkspaceModelInternal
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.coroutines.BazelCoroutineService

/**
 * Clean up any modules showing up due to the platform hack
 * https://youtrack.jetbrains.com/issue/IDEA-321160/Platform-solution-for-the-initial-state-of-the-project-model-on-the-first-open
 */
class CounterPlatformProjectConfigurator : DirectoryProjectConfigurator {
  override fun configureProject(
    project: Project,
    baseDir: VirtualFile,
    moduleRef: Ref<Module>,
    isProjectCreatedWithWizard: Boolean,
  ) = configureProject(project)

  fun configureProject(project: Project) {
    if (!project.isBazelProject || !Registry.`is`("ide.create.fake.module.on.project.import")) return

    val workspaceModel = WorkspaceModel.getInstance(project) as WorkspaceModelInternal
    val fakeModules =
      workspaceModel.entityStorage.current.entities(ModuleEntity::class.java)

    BazelCoroutineService.getInstance(project).start {
      writeAction {
        workspaceModel.updateProjectModel("Counter platform fake modules") {
          fakeModules.forEach { fakeModule -> it.removeEntity(fakeModule) }
        }
      }
    }
  }
}
