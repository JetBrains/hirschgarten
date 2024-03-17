package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleSourceDependency
import com.intellij.platform.workspace.jps.entities.SdkDependency
import com.intellij.platform.workspace.jps.entities.SdkId
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.PythonModule
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.PythonSdkInfo.Companion.PYTHON_SDK_ID

internal class PythonModuleWithSourcesUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
  private val isPythonSupportEnabled: Boolean,
) : WorkspaceModelEntityWithoutParentModuleUpdater<PythonModule, ModuleEntity> {
  override fun addEntity(entityToAdd: PythonModule): ModuleEntity {
    val moduleEntityUpdater =
      ModuleEntityUpdater(workspaceModelEntityUpdaterConfig, calculateModuleDefaultDependencies(entityToAdd))

    val moduleEntity = moduleEntityUpdater.addEntity(entityToAdd.module)

    val sourceEntityUpdater = SourceEntityUpdater(workspaceModelEntityUpdaterConfig)
    sourceEntityUpdater.addEntries(entityToAdd.sourceRoots, moduleEntity)

    val pythonResourceEntityUpdater = PythonResourceEntityUpdater(workspaceModelEntityUpdaterConfig)
    pythonResourceEntityUpdater.addEntries(entityToAdd.resourceRoots, moduleEntity)

    return moduleEntity
  }

  private fun calculateModuleDefaultDependencies(entityToAdd: PythonModule): List<ModuleDependencyItem> =
    if (isPythonSupportEnabled && entityToAdd.sdkInfo != null)
      defaultDependencies + SdkDependency(SdkId(entityToAdd.sdkInfo.toString(), PYTHON_SDK_ID))
    else
      defaultDependencies

  private companion object {
    val defaultDependencies = listOf(
      ModuleSourceDependency,
    )
  }
}

internal class PythonModuleWithoutSourcesUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithoutParentModuleUpdater<PythonModule, ModuleEntity> {
  override fun addEntity(entityToAdd: PythonModule): ModuleEntity {
    val moduleEntityUpdater = ModuleEntityUpdater(workspaceModelEntityUpdaterConfig)
    return moduleEntityUpdater.addEntity(entityToAdd.module)
  }
}

internal class PythonModuleUpdater(
  workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
  isPythonSupportEnabled: Boolean,
) : WorkspaceModelEntityWithoutParentModuleUpdater<PythonModule, ModuleEntity> {
  private val pythonModuleWithSourcesUpdater =
    PythonModuleWithSourcesUpdater(workspaceModelEntityUpdaterConfig, isPythonSupportEnabled)
  private val pythonModuleWithoutSourcesUpdater = PythonModuleWithoutSourcesUpdater(workspaceModelEntityUpdaterConfig)

  override fun addEntity(entityToAdd: PythonModule): ModuleEntity =
    when (Pair(entityToAdd.sourceRoots.size, entityToAdd.resourceRoots.size)) {
      Pair(0, 0) -> pythonModuleWithoutSourcesUpdater.addEntity(entityToAdd)
      else -> pythonModuleWithSourcesUpdater.addEntity(entityToAdd)
    }
}
