package org.jetbrains.plugins.bsp.android

import com.intellij.openapi.module.Module
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.workspaceModel.ide.impl.legacyBridge.module.findModuleEntity
import com.intellij.workspaceModel.ide.legacyBridge.ModuleBridge

internal val Module.moduleEntity: ModuleEntity?
  get() {
    val bridge = this as? ModuleBridge ?: return null
    return bridge.findModuleEntity(bridge.entityStorage.current)
  }
