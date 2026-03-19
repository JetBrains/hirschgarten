package org.jetbrains.bazel.sync.workspace.model

import org.jetbrains.bazel.commons.Tag
import org.jetbrains.bsp.protocol.BuildTargetTag

internal object BspMappings {
  fun toBspTag(tag: Tag): String? =
    when (tag) {
      Tag.APPLICATION -> null
      Tag.TEST -> null
      Tag.LIBRARY -> null
      Tag.INTELLIJ_PLUGIN -> "intellij-plugin"
      Tag.MANUAL -> BuildTargetTag.MANUAL
      Tag.NO_BUILD -> null
    }
}
