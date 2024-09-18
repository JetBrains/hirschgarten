package org.jetbrains.plugins.bsp.impl.magicmetamodel.impl.workspacemodel

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.workspaceModel.ide.impl.legacyBridge.module.findModuleEntity
import com.intellij.workspaceModel.ide.legacyBridge.ModuleBridge
import org.jetbrains.plugins.bsp.impl.utils.findModuleNameProvider
import org.jetbrains.plugins.bsp.impl.utils.orDefault
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo
import java.lang.reflect.Method

val Module.moduleEntity: ModuleEntity?
  get() {
    val bridge = this as? ModuleBridge ?: return null
    return bridge.findModuleEntity(bridge.entityStorage.current)
  }

fun BuildTargetInfo.getModule(project: Project): Module? {
  val moduleNameProvider = project.findModuleNameProvider().orDefault()
  val moduleName = moduleNameProvider(this)
  return ModuleManager.getInstance(project).findModuleByName(moduleName)
}

/**
 * WorkspaceModelInternal.getCanonicallyCasedVirtualFileUrlManager is added in 243 and not available in 242.
 * TODO replace with plain getCanonicallyCasedVirtualFileUrlManager after dropping 242.
 */
fun WorkspaceModel.bspVirtualFileUrlManager(): VirtualFileUrlManager =
  try {
    val method: Method = javaClass.getMethod("getCanonicallyCasedVirtualFileUrlManager")
    method.invoke(this) as VirtualFileUrlManager
  } catch (_: Exception) {
    this.getVirtualFileUrlManager()
  }
