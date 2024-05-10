package org.jetbrains.plugins.bsp.utils

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.extension.points.BuildTargetClassifierExtension
import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import org.jetbrains.plugins.bsp.extension.points.withBuildToolIdOrDefault
import org.jetbrains.plugins.bsp.magicmetamodel.DefaultModuleNameProvider
import org.jetbrains.plugins.bsp.magicmetamodel.TargetNameReformatProvider
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo

public fun Project.findModuleNameProvider(): TargetNameReformatProvider? =
  this.buildToolId.takeIf { it.id != "bsp" }?.let { createModuleNameProvider(it) }

public fun Project.findLibraryNameProvider(): TargetNameReformatProvider? =
  this.buildToolId.takeIf { it.id != "bsp" }?.let { createLibraryNameProvider(it) }

private fun createModuleNameProvider(buildToolId: BuildToolId): TargetNameReformatProvider =
  createNameReformatProvider(buildToolId)

private fun createLibraryNameProvider(buildToolId: BuildToolId): TargetNameReformatProvider =
  createNameReformatProvider(buildToolId)

private fun createNameReformatProvider(buildToolId: BuildToolId): (BuildTargetInfo) -> String {
  val bspBuildTargetClassifier = BuildTargetClassifierExtension.ep.withBuildToolIdOrDefault(buildToolId)
  return { buildTargetInfo ->
    val sanitizedName = bspBuildTargetClassifier.calculateBuildTargetName(buildTargetInfo).replaceDots()
    bspBuildTargetClassifier.calculateBuildTargetPath(buildTargetInfo)
      .shortenTargetPath()
      .joinToString(".", postfix = ".$sanitizedName") { pathElement -> pathElement.replaceDots() }
  }
}

private fun List<String>.shortenTargetPath(): List<String> =
  if (BspFeatureFlags.isShortenModuleLibraryNamesEnabled) {
    val maxLength = 200
    var runningLength = 0
    val (subPath, remaining) = asReversed().partition {
      runningLength += it.length
      runningLength <= maxLength
    }
    if (remaining.isEmpty()) subPath.asReversed()
    else listOf(StringUtils.md5Hash(remaining.joinToString(""), 5)) + subPath.asReversed()
  } else this

internal fun String.replaceDots(): String = this.replace('.', '-')

internal fun String.shortenTargetPath(): String =
  if (BspFeatureFlags.isShortenModuleLibraryNamesEnabled)
    split(".").shortenTargetPath().joinToString(".")
  else this

public fun TargetNameReformatProvider?.orDefault(): TargetNameReformatProvider = this ?: DefaultModuleNameProvider
