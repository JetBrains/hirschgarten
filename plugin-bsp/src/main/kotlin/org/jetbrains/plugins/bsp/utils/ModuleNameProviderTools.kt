package org.jetbrains.plugins.bsp.utils

import com.intellij.openapi.project.Project
import org.jetbrains.magicmetamodel.DefaultModuleNameProvider
import org.jetbrains.magicmetamodel.ModuleNameProvider
import org.jetbrains.plugins.bsp.extension.points.BspBuildTargetClassifierExtension
import org.jetbrains.plugins.bsp.server.connection.BspConnectionService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.BspBuildTargetClassifierProvider

private fun obtainToolNameIfKnown(project: Project): String? =
  BspConnectionService.getInstance(project).value?.buildToolId

private fun createModuleNameProvider(toolName: String): ModuleNameProvider {
  val targetClassifier =
    BspBuildTargetClassifierProvider(toolName, BspBuildTargetClassifierExtension.extensions())
  return {
    targetClassifier.getBuildTargetPath(it)
      .joinToString(".", postfix = ".${targetClassifier.getBuildTargetName(it)}")
  }
}

public fun Project.findModuleNameProvider(): ModuleNameProvider? =
  obtainToolNameIfKnown(this)?.let(::createModuleNameProvider)

public fun ModuleNameProvider?.orDefault(): ModuleNameProvider = this ?: DefaultModuleNameProvider
