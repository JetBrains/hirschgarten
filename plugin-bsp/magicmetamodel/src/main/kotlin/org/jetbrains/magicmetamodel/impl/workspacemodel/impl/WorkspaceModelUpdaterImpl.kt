package org.jetbrains.magicmetamodel.impl.workspacemodel.impl

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.workspaceModel.storage.MutableEntityStorage
import com.intellij.workspaceModel.storage.url.VirtualFileUrlManager
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleName
import org.jetbrains.magicmetamodel.impl.workspacemodel.WorkspaceModelUpdater
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaModuleUpdater
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.WorkspaceModelEntityUpdaterConfig
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.WorkspaceModuleRemover
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ModuleDetailsToJavaModuleTransformer

internal class WorkspaceModelUpdaterImpl(
  workspaceEntityStorageBuilder: MutableEntityStorage,
  val virtualFileUrlManager: VirtualFileUrlManager,
  moduleNameProvider: ((BuildTargetIdentifier) -> String)?
) : WorkspaceModelUpdater {

  private val workspaceModelEntityUpdaterConfig = WorkspaceModelEntityUpdaterConfig(
    workspaceEntityStorageBuilder = workspaceEntityStorageBuilder,
    virtualFileUrlManager = virtualFileUrlManager,
  )
  private val javaModuleUpdater = JavaModuleUpdater(workspaceModelEntityUpdaterConfig)
  private val workspaceModuleRemover = WorkspaceModuleRemover(workspaceModelEntityUpdaterConfig)
  private val moduleDetailsToJavaModuleTransformer = ModuleDetailsToJavaModuleTransformer(moduleNameProvider)

  override fun loadModule(moduleDetails: ModuleDetails) {
    // TODO for now we are supporting only java modules
    val javaModule = moduleDetailsToJavaModuleTransformer.transform(moduleDetails)

    javaModuleUpdater.addEntity(javaModule)
  }

  override fun removeModule(module: ModuleName) {
    workspaceModuleRemover.removeEntity(module)
  }

  override fun clear() {
    workspaceModuleRemover.clear()
  }
}
