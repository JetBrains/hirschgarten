package org.jetbrains.bazel.flow.open

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.registry.RegistryManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.DirectoryProjectConfigurator
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.util.concurrency.annotations.RequiresWriteLock
import com.intellij.workspaceModel.ide.JpsProjectLoadingManager
import org.jetbrains.bazel.config.BazelProjectProperties

/**
 * Clean up any modules showing up due to the platform hack
 * https://youtrack.jetbrains.com/issue/IDEA-321160/Platform-solution-for-the-initial-state-of-the-project-model-on-the-first-open
 */
private class CounterPlatformProjectConfigurator : DirectoryProjectConfigurator.AsyncDirectoryProjectConfigurator() {
  override suspend fun configure(
    project: Project,
    baseDir: VirtualFile,
    moduleRef: Ref<Module>,
    isProjectCreatedWithWizard: Boolean,
  ) {
    configureProjectCounterPlatform(project)
  }
}

internal suspend fun configureProjectCounterPlatform(project: Project) {
  removeFakeModulesAndLibraries(project)
  // https://youtrack.jetbrains.com/issue/BAZEL-1933
  project.serviceAsync<JpsProjectLoadingManager>().jpsProjectLoaded {
    removeFakeModulesAndLibrariesBlocking(project)
  }
}

private suspend fun removeFakeModulesAndLibraries(project: Project) {
  if (!project.serviceAsync<BazelProjectProperties>().isBazelProject ||
    !RegistryManager.getInstanceAsync().`is`("ide.create.fake.module.on.project.import")
  ) {
    return
  }

  val workspaceModel = project.serviceAsync<WorkspaceModel>()
  writeAction {
    updateProjectModel(workspaceModel)
  }
}

private fun removeFakeModulesAndLibrariesBlocking(project: Project) {
  if (!project.service<BazelProjectProperties>().isBazelProject || !Registry.`is`("ide.create.fake.module.on.project.import", true)) {
    return
  }

  val workspaceModel = WorkspaceModel.getInstance(project)
  WriteAction.runAndWait<Throwable> {
    updateProjectModel(workspaceModel)
  }
}

/**
 * Not using asynchronous [WorkspaceModel.update] here to be extra sure that scanning can't start in between
 */
@RequiresWriteLock
private fun updateProjectModel(workspaceModel: WorkspaceModel) {
  @Suppress("UsagesOfObsoleteApi")
  workspaceModel.updateProjectModel("Remove fake modules and libraries from the project") { storage ->
    storage.entities(ModuleEntity::class.java).forEach { storage.removeEntity(it) }
    storage.entities(LibraryEntity::class.java).forEach { storage.removeEntity(it) }
  }
}
