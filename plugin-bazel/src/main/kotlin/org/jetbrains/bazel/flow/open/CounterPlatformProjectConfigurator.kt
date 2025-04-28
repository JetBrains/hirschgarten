package org.jetbrains.bazel.flow.open

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.DirectoryProjectConfigurator
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.impl.WorkspaceModelInternal
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.workspaceModel.ide.JpsProjectLoadingManager
import org.jetbrains.bazel.config.isBazelProject

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
    removeFakeModulesAndLibraries(project)
    // https://youtrack.jetbrains.com/issue/BAZEL-1933
    JpsProjectLoadingManager.getInstance(project).jpsProjectLoaded {
      removeFakeModulesAndLibraries(project)
    }
  }
}

private fun removeFakeModulesAndLibraries(project: Project) {
  if (!project.isBazelProject || !Registry.`is`("ide.create.fake.module.on.project.import", true)) return

  val workspaceModel = WorkspaceModel.getInstance(project) as WorkspaceModelInternal
  WriteAction.runAndWait<Throwable> {
    workspaceModel.updateProjectModel("Remove fake modules and libraries from the project") { storage ->
      storage.entities(ModuleEntity::class.java).forEach { storage.removeEntity(it) }
      storage.entities(LibraryEntity::class.java).forEach { storage.removeEntity(it) }
    }
  }
}
