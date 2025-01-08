package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.util

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.workspaceModel.ide.impl.legacyBridge.module.findModuleEntity
import com.intellij.workspaceModel.ide.legacyBridge.ModuleBridge
import org.jetbrains.plugins.bsp.magicmetamodel.findNameProvider
import org.jetbrains.plugins.bsp.magicmetamodel.orDefault
import org.jetbrains.plugins.bsp.target.TemporaryTargetUtils
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo

val Module.moduleEntity: ModuleEntity?
  get() {
    val bridge = this as? ModuleBridge ?: return null
    return bridge.findModuleEntity(bridge.entityStorage.current)
  }

fun BuildTargetInfo.getModule(project: Project): Module? {
  val moduleNameProvider = project.findNameProvider().orDefault()
  val moduleName = moduleNameProvider(this)
  return ModuleManager.getInstance(project).findModuleByName(moduleName)
}

fun BuildTargetIdentifier.getModule(project: Project): Module? =
  project.service<TemporaryTargetUtils>().getBuildTargetInfoForId(this)?.getModule(project)

fun BuildTargetIdentifier.getModuleEntity(project: Project): ModuleEntity? = getModule(project)?.moduleEntity
