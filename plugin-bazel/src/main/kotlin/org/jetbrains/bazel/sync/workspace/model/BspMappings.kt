package org.jetbrains.bazel.sync.workspace.model

import org.jetbrains.bazel.commons.Tag
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.mapper.BazelMappedProject
import org.jetbrains.bsp.protocol.BuildTargetTag

object BspMappings {
  fun toBspTag(tag: Tag): String? =
    when (tag) {
      Tag.APPLICATION -> null
      Tag.TEST -> null
      Tag.LIBRARY -> null
      Tag.INTELLIJ_PLUGIN -> "intellij-plugin"
      Tag.NO_IDE -> BuildTargetTag.NO_IDE
      Tag.MANUAL -> BuildTargetTag.MANUAL
      Tag.LIBRARIES_OVER_MODULES -> BuildTargetTag.LIBRARIES_OVER_MODULES
      Tag.NO_BUILD -> null
      Tag.IDE_LOW_SHARED_SOURCES_PRIORITY -> null
    }

  fun getModules(project: BazelMappedProject, targets: List<Label>): Set<Module> = targets.mapNotNull(project::findModule).toSet()
}
