package org.jetbrains.bazel.server.model

import org.jetbrains.bazel.label.Label
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
      Tag.NO_BUILD -> null
    }

  fun getModules(project: AspectSyncProject, targets: List<Label>): Set<Module> = targets.mapNotNull(project::findModule).toSet()
}
