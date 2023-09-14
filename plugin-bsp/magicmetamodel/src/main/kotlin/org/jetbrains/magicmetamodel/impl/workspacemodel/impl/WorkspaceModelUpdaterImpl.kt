package org.jetbrains.magicmetamodel.impl.workspacemodel.impl

import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryRoot
import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId
import com.intellij.platform.workspace.jps.entities.LibraryTableId
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.magicmetamodel.impl.workspacemodel.JavaModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.Library
import org.jetbrains.magicmetamodel.impl.workspacemodel.Module
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleName
import org.jetbrains.magicmetamodel.impl.workspacemodel.PythonModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.WorkspaceModelUpdater
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.BspEntitySource
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaModuleUpdater
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.PythonModuleUpdater
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.WorkspaceModelEntityUpdaterConfig
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.WorkspaceModuleRemover
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.JavaModuleToDummyJavaModulesTransformerHACK
import java.net.URI
import java.nio.file.Path

internal class WorkspaceModelUpdaterImpl(
  workspaceEntityStorageBuilder: MutableEntityStorage,
  val virtualFileUrlManager: VirtualFileUrlManager,
  projectBasePath: Path,
  isPythonSupportEnabled: Boolean,
) : WorkspaceModelUpdater {
  private val workspaceModelEntityUpdaterConfig = WorkspaceModelEntityUpdaterConfig(
    workspaceEntityStorageBuilder = workspaceEntityStorageBuilder,
    virtualFileUrlManager = virtualFileUrlManager,
    projectBasePath = projectBasePath,
  )
  private val javaModuleUpdater = JavaModuleUpdater(workspaceModelEntityUpdaterConfig, projectBasePath)
  private val pythonModuleUpdater = PythonModuleUpdater(workspaceModelEntityUpdaterConfig, isPythonSupportEnabled)

  private val workspaceModuleRemover = WorkspaceModuleRemover(workspaceModelEntityUpdaterConfig)
  private val javaModuleToDummyJavaModulesTransformerHACK =
    JavaModuleToDummyJavaModulesTransformerHACK(projectBasePath)

  override fun loadModule(module: Module) {
    when (module) {
      is JavaModule -> {
        val dummyJavaModules = javaModuleToDummyJavaModulesTransformerHACK.transform(module)
        javaModuleUpdater.addEntries(dummyJavaModules.filterNot { it.isAlreadyAdded() })
        javaModuleUpdater.addEntity(module)
      }
      is PythonModule -> pythonModuleUpdater.addEntity(module)
    }
  }

  override fun loadLibraries(libraries: List<Library>) {
    libraries.forEach { entityToAdd ->
      workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.addEntity(
        LibraryEntity(
          name = entityToAdd.displayName,
          tableId = LibraryTableId.ProjectLibraryTableId,
          roots = entityToAdd.classJars.map {
            LibraryRoot(
              url = workspaceModelEntityUpdaterConfig.virtualFileUrlManager.fromUrl("jar://${URI.create(it).path}!/"),
              type = LibraryRootTypeId.COMPILED,
            )
          },
          entitySource = BspEntitySource,
        ) {
          this.excludedRoots = arrayListOf()
        },
      )
    }
  }

  private fun Module.isAlreadyAdded() =
    workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.contains(ModuleId(getModuleName()))

  override fun removeModule(module: ModuleName) {
    workspaceModuleRemover.removeEntity(module)
  }

  override fun clear() {
    workspaceModuleRemover.clear()
  }
}
