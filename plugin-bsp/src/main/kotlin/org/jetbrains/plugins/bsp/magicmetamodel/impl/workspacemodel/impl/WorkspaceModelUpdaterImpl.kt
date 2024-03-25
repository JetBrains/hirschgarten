package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl

import com.intellij.openapi.project.Project
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryRoot
import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId
import com.intellij.platform.workspace.jps.entities.LibraryTableId
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.workspaceModel.ide.impl.LegacyBridgeJpsEntitySourceFactory
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.GoModule
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.JavaModule
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.Library
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.Module
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.ModuleName
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.PythonModule
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.WorkspaceModelUpdater
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.GoModuleUpdater
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.JavaModuleUpdater
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.PythonModuleUpdater
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.WorkspaceModelEntityUpdaterConfig
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.WorkspaceModuleRemover
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.JavaModuleToDummyJavaModulesTransformerHACK
import org.jetbrains.workspacemodel.entities.BspEntitySource
import org.jetbrains.workspacemodel.entities.BspProjectDirectoriesEntity
import java.nio.file.Path

internal class WorkspaceModelUpdaterImpl(
  workspaceEntityStorageBuilder: MutableEntityStorage,
  val virtualFileUrlManager: VirtualFileUrlManager,
  projectBasePath: Path,
  project: Project,
  isPythonSupportEnabled: Boolean,
  isAndroidSupportEnabled: Boolean,
) : WorkspaceModelUpdater {
  private val workspaceModelEntityUpdaterConfig = WorkspaceModelEntityUpdaterConfig(
    workspaceEntityStorageBuilder = workspaceEntityStorageBuilder,
    virtualFileUrlManager = virtualFileUrlManager,
    projectBasePath = projectBasePath,
    project = project,
  )
  private val javaModuleUpdater =
    JavaModuleUpdater(workspaceModelEntityUpdaterConfig, projectBasePath, isAndroidSupportEnabled)
  private val pythonModuleUpdater = PythonModuleUpdater(workspaceModelEntityUpdaterConfig, isPythonSupportEnabled)
  private val goModuleUpdater = GoModuleUpdater(workspaceModelEntityUpdaterConfig)

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
      is GoModule -> goModuleUpdater.addEntity(module)
    }
  }

  override fun loadLibraries(libraries: List<Library>) {
    libraries.forEach { entityToAdd ->
      workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.addEntity(
        LibraryEntity(
          name = entityToAdd.displayName,
          tableId = LibraryTableId.ProjectLibraryTableId,
          roots = entityToAdd.toJarOrIJar(),
          entitySource = LegacyBridgeJpsEntitySourceFactory.createEntitySourceForProjectLibrary(
            project = workspaceModelEntityUpdaterConfig.project,
            externalSource = null,
          ),
        ) {
          this.excludedRoots = arrayListOf()
        },
      )
    }
  }

  private fun Library.toJarOrIJar(): List<LibraryRoot> =
    classJars.ifEmpty { iJars }.toLibraryRootsOfType(LibraryRootTypeId.COMPILED)

  private fun List<String>.toLibraryRootsOfType(type: LibraryRootTypeId) = map {
    LibraryRoot(
      url = workspaceModelEntityUpdaterConfig.virtualFileUrlManager.getOrCreateFromUri(Library.formatJarString(it)),
      type = type,
    )
  }

  override fun loadDirectories(includedDirectories: List<VirtualFileUrl>, excludedDirectories: List<VirtualFileUrl>) {
    val entity = BspProjectDirectoriesEntity(
      projectRoot = workspaceModelEntityUpdaterConfig.projectBasePath
        .toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
      includedRoots = includedDirectories,
      excludedRoots = excludedDirectories,
      entitySource = BspEntitySource,
    )
    workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.addEntity(entity)
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
