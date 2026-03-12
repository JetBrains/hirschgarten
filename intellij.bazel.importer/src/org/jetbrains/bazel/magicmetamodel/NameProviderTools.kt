package org.jetbrains.bazel.magicmetamodel

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.languages.starlark.repomapping.toApparentLabelOrThis
import org.jetbrains.bsp.protocol.utils.StringUtils

@ApiStatus.Internal
fun Label.formatAsModuleName(project: Project): String {
  val targetName = targetName.sanitizeName()
  val prefix =
    this.toApparentLabelOrThis(project)
      .let { listOf((it as? ResolvedLabel)?.repoName.orEmpty()) + it.packagePath.pathSegments }
      .filter { pathSegment -> pathSegment.isNotEmpty() }
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

internal fun String.sanitizeName(): String {
  val name = replaceDots().filterColon()
  // See BAZEL-3021 for details
  // On the master branch, the fix is on the DevKit plugin side:
  // https://github.com/JetBrains/intellij-community/commit/5668ed2d8ba84a84486b1adc6c0be4901f6c3146
  // However, with 261 code freeze in place, it's nearly impossible to get QA approval to cherry-pick this change to 261,
  // so it must be a hack on the Bazel plugin side.
  val rulesJvmSuffixes = listOf("__jps", "__kt")
  for (prefix in rulesJvmSuffixes) {
    if (name.startsWith("_") && name.endsWith(prefix)) {
      return name.substring(1, name.length - prefix.length)
    }
  }
  return name
}

private fun String.replaceDots(): String = this.replace('.', '-')

/**
 * for Windows compatibility, e.g. C:
 */
private fun String.filterColon(): String = this.replace(":", "")

internal fun String.shortenTargetPath(): String =
  if (BazelFeatureFlags.isShortenModuleLibraryNamesEnabled) {
    split(".").shortenTargetPath().joinToString(".")
  } else {
    this
  }
