package org.jetbrains.bazel.magicmetamodel

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.languages.starlark.repomapping.toApparentLabelOrThis
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.sync.workspace.snapshot.shortChecksum
import org.jetbrains.bsp.protocol.utils.StringUtils

@ApiStatus.Internal
val LIBRARY_MODULE_PREFIX: String = "_aux.libraries."

// TODO: I really don't like `formatAsModuleName`, it suppose to construct `ModuleEntity`/`LibraryEntity`
//  name from label, but it misused e.g., in TargetUtils by using it to create key in module to target map,
//  this is bad because we end up with multiple places that "define" global module name, it isn't great
//  ideal solution would include obtaining those names only within backend modules and using those values as
//  source of truth

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

@ApiStatus.Internal
fun Label.formatAsModuleName(repoMapping: RepoMapping): String {
  val targetName = targetName.sanitizeName()
  val prefix =
    this.toApparentLabelOrThis(repoMapping)
      .let { listOf((it as? ResolvedLabel)?.repoName.orEmpty()) + it.packagePath.pathSegments }
      .filter { pathSegment -> pathSegment.isNotEmpty() }
      .shortenTargetPath(targetName.length)
      .joinToString(".") { pathElement -> pathElement.sanitizeName() }
  return if (prefix.isBlank()) targetName else "$prefix.$targetName"
}

@ApiStatus.Internal
fun WorkspaceTargetKey.formatAsModuleName(repoMapping: RepoMapping, withConfiguration: Boolean = true): String =
  if (withConfiguration) {
    val checksum = configuration.shortChecksum ?: "default"
    "${label.formatAsModuleName(repoMapping)}-$checksum"
  }
  else {
    label.formatAsModuleName(repoMapping)
  }

@ApiStatus.Internal
fun WorkspaceTargetKey.formatAsLibraryName(repoMapping: RepoMapping, withFullKey: Boolean = true): String {
  val base = label.formatAsModuleName(repoMapping)
  return if (withFullKey) {
    val aspectChecksum = aspectIds.ids.takeIf { it.isNotEmpty() }
      ?.let { StringUtils.md5Hash(it.joinToString(","), 5) }
    val suffix = listOfNotNull(configuration.shortChecksum, aspectChecksum).joinToString("-")
    if (suffix.isEmpty()) {
      base
    }
    else {
      "$base-$suffix"
    }
  }
  else {
    base
  }
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
    }
    else {
      listOf(StringUtils.md5Hash(remaining.joinToString(""), 5)) + subPath.asReversed()
    }
  }
  else {
    this
  }

@ApiStatus.Internal
fun String.sanitizeName(): String = replaceDots().filterColon()

private fun String.replaceDots(): String = this.replace('.', '-')

/**
 * for Windows compatibility, e.g. C:
 */
private fun String.filterColon(): String = this.replace(":", "")

@ApiStatus.Internal
fun String.shortenTargetPath(): String =
  if (BazelFeatureFlags.isShortenModuleLibraryNamesEnabled) {
    split(".").shortenTargetPath().joinToString(".")
  }
  else {
    this
  }
