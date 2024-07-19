package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.workspaceModel.ide.impl.legacyBridge.module.findModuleEntity
import com.intellij.workspaceModel.ide.legacyBridge.ModuleBridge
import org.jetbrains.plugins.bsp.utils.findModuleNameProvider
import org.jetbrains.plugins.bsp.utils.orDefault

internal val Module.moduleEntity: ModuleEntity?
  get() {
    val bridge = this as? ModuleBridge ?: return null
    return bridge.findModuleEntity(bridge.entityStorage.current)
  }

internal fun BuildTargetInfo.getModule(project: Project): Module? {
  val moduleNameProvider = project.findModuleNameProvider().orDefault()
  val moduleName = moduleNameProvider(this)
  return ModuleManager.getInstance(project).findModuleByName(moduleName)
}
