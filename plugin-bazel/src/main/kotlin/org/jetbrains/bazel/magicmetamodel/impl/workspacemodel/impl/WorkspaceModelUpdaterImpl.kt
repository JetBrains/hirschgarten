package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl

import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.project.Project
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.WorkspaceModelUpdater
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.CompiledSourceCodeInsideJarExcludeEntityUpdater
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.JavaModuleUpdater
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.LibraryEntityUpdater
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.WorkspaceModelEntityUpdaterConfig
import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExclude
import org.jetbrains.bazel.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.workspacemodel.entities.Library
import org.jetbrains.bazel.workspacemodel.entities.Module
import java.nio.file.Path

class WorkspaceModelUpdaterImpl(
  workspaceEntityStorageBuilder: MutableEntityStorage,
  val virtualFileUrlManager: VirtualFileUrlManager,
  projectBasePath: Path,
  project: Project,
  isAndroidSupportEnabled: Boolean,
  libraryModules: List<JavaModule> = emptyList(),
) : WorkspaceModelUpdater {
  private val workspaceModelEntityUpdaterConfig =
    WorkspaceModelEntityUpdaterConfig(
      workspaceEntityStorageBuilder = workspaceEntityStorageBuilder,
      virtualFileUrlManager = virtualFileUrlManager,
      projectBasePath = projectBasePath,
      project = project,
    )
  private val javaModuleUpdater =
    JavaModuleUpdater(workspaceModelEntityUpdaterConfig, projectBasePath, isAndroidSupportEnabled, libraryModules)

  init {
    // store generated IML files outside the project directory
    ExternalProjectsManagerImpl.getInstance(project).setStoreExternally(true)
  }

  override suspend fun loadModules(moduleEntities: List<Module>) {
    moduleEntities.forEach { loadModule(it) }
  }

  private suspend fun loadModule(module: Module) {
    when (module) {
      is JavaModule -> {
        javaModuleUpdater.addEntity(module)
      }
    }
  }

  override suspend fun loadLibraries(libraries: List<Library>) {
    val libraryEntityUpdater = LibraryEntityUpdater(workspaceModelEntityUpdaterConfig)
    libraryEntityUpdater.addEntities(libraries)
  }

  override suspend fun loadCompiledSourceCodeInsideJarExclude(exclude: CompiledSourceCodeInsideJarExclude) {
    val updater = CompiledSourceCodeInsideJarExcludeEntityUpdater(workspaceModelEntityUpdaterConfig)
    updater.addEntity(exclude)
  }
}
