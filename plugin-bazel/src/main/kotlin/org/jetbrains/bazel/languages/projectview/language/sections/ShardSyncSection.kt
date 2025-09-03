package org.jetbrains.bazel.languages.projectview.language.sections

import org.jetbrains.bazel.languages.projectview.language.SectionKey
import org.jetbrains.bazel.languages.projectview.language.sections.presets.BooleanScalarSection

class ShardSyncSection : BooleanScalarSection() {
  override val name = NAME
  override val sectionKey: SectionKey<Boolean> = KEY
  override val default = false
  override val doc =
    "Directs the plugin to shard bazel build invocations when syncing " +
      "and compiling your project. Bazel builds for sync may be sharded even if " +
      "this is set to false, to keep the build command under the maximum command length (ARG_MAX)."

  companion object {
    const val NAME = "shard_sync"
    val KEY = SectionKey<Boolean>(NAME)
  }
}
