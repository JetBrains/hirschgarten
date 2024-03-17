package org.jetbrains.plugins.bsp.utils

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.extension.points.BuildTargetClassifierExtension
import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import org.jetbrains.plugins.bsp.extension.points.withBuildToolIdOrDefault
import org.jetbrains.plugins.bsp.magicmetamodel.DefaultModuleNameProvider
import org.jetbrains.plugins.bsp.magicmetamodel.ModuleNameProvider

public fun Project.findModuleNameProvider(): ModuleNameProvider? =
  this.buildToolId.takeIf { it.id != "bsp" }?.let { createModuleNameProvider(it) }

private fun createModuleNameProvider(buildToolId: BuildToolId): ModuleNameProvider {
  val bspBuildTargetClassifier = BuildTargetClassifierExtension.ep.withBuildToolIdOrDefault(buildToolId)
  return {
    val sanitizedName = bspBuildTargetClassifier.calculateBuildTargetName(it).replaceDots()
    bspBuildTargetClassifier.calculateBuildTargetPath(it)
      .joinToString(".", postfix = ".$sanitizedName") { pathElement -> pathElement.replaceDots() }
  }
}

private fun String.replaceDots(): String = this.replace('.', ' ')

public fun ModuleNameProvider?.orDefault(): ModuleNameProvider = this ?: DefaultModuleNameProvider
