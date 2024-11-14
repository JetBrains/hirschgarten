package org.jetbrains.plugins.bsp.magicmetamodel

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.config.buildToolIdOrDefault
import org.jetbrains.plugins.bsp.config.withBuildToolIdOrDefault
import org.jetbrains.plugins.bsp.extensionPoints.BuildTargetClassifierExtension
import org.jetbrains.plugins.bsp.utils.StringUtils
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo

fun Project.findNameProvider(): TargetNameReformatProvider? =
  this.buildToolIdOrDefault.takeIf { it.id != "bsp" }?.let { createModuleNameProvider(it) }

private fun createModuleNameProvider(buildToolId: BuildToolId): TargetNameReformatProvider = createNameReformatProvider(buildToolId)

private fun createNameReformatProvider(buildToolId: BuildToolId): (BuildTargetInfo) -> String {
  val bspBuildTargetClassifier = BuildTargetClassifierExtension.ep.withBuildToolIdOrDefault(buildToolId)
  return { buildTargetInfo ->
    val sanitizedName = bspBuildTargetClassifier.calculateBuildTargetName(buildTargetInfo).replaceDots()
    val prefix =
      bspBuildTargetClassifier
        .calculateBuildTargetPath(buildTargetInfo)
        .shortenTargetPath(sanitizedName.length)
        .joinToString(".") { pathElement -> pathElement.replaceDots() }
    if (prefix.isBlank()) sanitizedName else "$prefix.$sanitizedName"
  }
}

private fun List<String>.shortenTargetPath(targetNameLength: Int = 0): List<String> =
  if (BspFeatureFlags.isShortenModuleLibraryNamesEnabled) {
    val maxLength = 200 - targetNameLength
    var runningLength = 0
    val (subPath, remaining) =
      asReversed().partition {
        runningLength += it.length
        runningLength <= maxLength
      }
    if (remaining.isEmpty()) {
      subPath.asReversed()
    } else {
      listOf(StringUtils.md5Hash(remaining.joinToString(""), 5)) + subPath.asReversed()
    }
  } else {
    this
  }

fun String.replaceDots(): String = this.replace('.', '-')

fun String.shortenTargetPath(): String =
  if (BspFeatureFlags.isShortenModuleLibraryNamesEnabled) {
    split(".").shortenTargetPath().joinToString(".")
  } else {
    this
  }

fun TargetNameReformatProvider?.orDefault(): TargetNameReformatProvider = this ?: DefaultNameProvider
