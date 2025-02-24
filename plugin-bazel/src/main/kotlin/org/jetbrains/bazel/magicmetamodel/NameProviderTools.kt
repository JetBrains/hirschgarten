package org.jetbrains.bazel.magicmetamodel

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BspFeatureFlags
import org.jetbrains.bazel.extensionPoints.BazelBuildTargetClassifier
import org.jetbrains.bazel.utils.StringUtils

fun Project.findNameProvider(): TargetNameReformatProvider =
  { buildTargetInfo ->
    val targetName = BazelBuildTargetClassifier.calculateBuildTargetName(buildTargetInfo).sanitizeName()
    val prefix =
      BazelBuildTargetClassifier
        .calculateBuildTargetPath(buildTargetInfo)
        .shortenTargetPath(targetName.length)
        .joinToString(".") { pathElement -> pathElement.sanitizeName() }
    if (prefix.isBlank()) targetName else "$prefix.$targetName"
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

fun String.sanitizeName(): String = replaceDots().filterColon()

private fun String.replaceDots(): String = this.replace('.', '-')

/**
 * for Windows compatibility, e.g. C:
 */
private fun String.filterColon(): String = this.replace(":", "")

fun String.shortenTargetPath(): String =
  if (BspFeatureFlags.isShortenModuleLibraryNamesEnabled) {
    split(".").shortenTargetPath().joinToString(".")
  } else {
    this
  }

fun TargetNameReformatProvider?.orDefault(): TargetNameReformatProvider = this ?: DefaultNameProvider
