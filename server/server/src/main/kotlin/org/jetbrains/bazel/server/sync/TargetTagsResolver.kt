package org.jetbrains.bazel.server.sync

import org.jetbrains.bazel.info.TargetInfo
import org.jetbrains.bazel.server.model.Tag
import org.jetbrains.bazel.workspacecontext.WorkspaceContext

private val bazelTagToTagMapping =
  mapOf(
    "no-ide" to Tag.NO_IDE,
    "manual" to Tag.MANUAL,
    "ide-low-shared-sources-priority" to Tag.IDE_LOW_SHARED_SOURCES_PRIORITY,
  )

class TargetTagsResolver {
  fun resolveTags(targetInfo: TargetInfo, workspaceContext: WorkspaceContext): Set<Tag> {
    val typeTags =
      when {
        targetInfo.isTest() -> setOf(Tag.TEST)
        targetInfo.isIntellijPlugin() -> setOf(Tag.INTELLIJ_PLUGIN, Tag.APPLICATION)
        targetInfo.isApplication() || targetInfo.isAndroidBinary() -> setOf(Tag.APPLICATION)
        else -> setOf(Tag.LIBRARY)
      }

    return typeTags + mapBazelTags(targetInfo.tags) + targetInfo.experimentalTags(workspaceContext)
  }

  private fun TargetInfo.experimentalTags(workspaceContext: WorkspaceContext): Set<Tag> =
    buildSet {
      if (kind in workspaceContext.experimentalPrioritizeLibrariesOverModulesTargetKinds.values) add(Tag.LIBRARIES_OVER_MODULES)
    }

  // https://bazel.build/extending/rules#executable_rules_and_test_rules:
  // "Test rules must have names that end in _test."
  private fun TargetInfo.isTest(): Boolean = isApplication() && kind.endsWith("_test")

  private fun TargetInfo.isIntellijPlugin(): Boolean = kind == "intellij_plugin_debug_target"

  // Not marked as executable by Bazel, but is actually executable via bazel mobile-install
  private fun TargetInfo.isAndroidBinary(): Boolean = kind == "android_binary"

  private fun TargetInfo.isApplication(): Boolean = executable

  private fun mapBazelTags(tags: List<String>): Set<Tag> = tags.mapNotNull { bazelTagToTagMapping[it] }.toSet()
}
