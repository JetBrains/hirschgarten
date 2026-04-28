package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl

import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.CompiledSourceCodeInsideJarExcludeEntityUpdater
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.JavaModuleUpdater
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.LibraryEntityUpdater
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.WorkspaceModelEntityUpdaterConfig
import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExclude
import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeEntity
import org.jetbrains.bazel.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.workspacemodel.entities.Library
import org.jetbrains.bazel.workspacemodel.entities.Module
import java.nio.file.Path

@ApiStatus.Internal
class WorkspaceModelUpdater(
  workspaceEntityStorageBuilder: MutableEntityStorage,
  private val virtualFileUrlManager: VirtualFileUrlManager,
  private val projectBasePath: Path,
  private val importIjars: Boolean,
  private val entitySource: EntitySource,
  private val currentExcludeEntity: CompiledSourceCodeInsideJarExcludeEntity?
) {
  private val workspaceModelEntityUpdaterConfig =
    WorkspaceModelEntityUpdaterConfig(
      workspaceEntityStorageBuilder = workspaceEntityStorageBuilder,
      virtualFileUrlManager = virtualFileUrlManager,
      projectBasePath = projectBasePath,
      entitySource = entitySource,
    )

  suspend fun load(
    moduleEntities: List<Module>,
    libraries: List<Library>,
  ) {
    val javaModuleUpdater =
      JavaModuleUpdater(
        workspaceModelEntityUpdaterConfig,
        projectBasePath,
        moduleEntities,
        libraries,
      )
    javaModuleUpdater.addEntities(moduleEntities.filterIsInstance<JavaModule>())
    val libraryEntityUpdater = LibraryEntityUpdater(workspaceModelEntityUpdaterConfig, importIjars)
    libraryEntityUpdater.addEntities(libraries)
  }

  suspend fun loadCompiledSourceCodeInsideJarExclude(exclude: CompiledSourceCodeInsideJarExclude) {
    val updater = CompiledSourceCodeInsideJarExcludeEntityUpdater(workspaceModelEntityUpdaterConfig, currentExcludeEntity)
    updater.addEntity(exclude)
  }
}
