package org.jetbrains.bazel.magicmetamodel

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.extensionPoints.buildTargetClassifier.TreeTargetClassifier
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.utils.StringUtils

fun Label.formatAsModuleName(project: Project): String {
  val treeTargetClassifier = TreeTargetClassifier(project)
  val targetName = treeTargetClassifier.calculateBuildTargetName(this).sanitizeName()
  val prefix =
    treeTargetClassifier
      .calculateBuildTargetPath(this)
      .shortenTargetPath(targetName.length)
      .joinToString(".") { pathElement -> pathElement.sanitizeName() }
  return if (prefix.isBlank()) targetName else "$prefix.$targetName"
}

private fun List<String>.shortenTargetPath(targetNameLength: Int = 0): List<String> =
  if (BazelFeatureFlags.isShortenModuleLibraryNamesEnabled) {
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
  if (BazelFeatureFlags.isShortenModuleLibraryNamesEnabled) {
    split(".").shortenTargetPath().joinToString(".")
  } else {
    this
  }
