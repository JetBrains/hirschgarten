package org.jetbrains.plugins.bsp.utils

import com.intellij.openapi.project.Project
import org.jetbrains.magicmetamodel.DefaultModuleNameProvider
import org.jetbrains.magicmetamodel.ModuleNameProvider
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.extension.points.BspBuildTargetClassifierExtension
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.BspBuildTargetClassifierProvider

public fun Project.findModuleNameProvider(): ModuleNameProvider? =
  this.buildToolId.takeIf { it.id != "bsp" }?.let { createModuleNameProvider(it.id) }

private fun createModuleNameProvider(toolName: String): ModuleNameProvider {
  val targetClassifier =
    BspBuildTargetClassifierProvider(toolName, BspBuildTargetClassifierExtension.extensions())
  return {
    val sanitizedName = targetClassifier.getBuildTargetName(it).replaceDots()
    targetClassifier.getBuildTargetPath(it)
      .joinToString(".", postfix = ".$sanitizedName") { pathElement -> pathElement.replaceDots() }
  }
}

private fun String.replaceDots(): String = this.replace('.', ' ')

public fun ModuleNameProvider?.orDefault(): ModuleNameProvider = this ?: DefaultModuleNameProvider
