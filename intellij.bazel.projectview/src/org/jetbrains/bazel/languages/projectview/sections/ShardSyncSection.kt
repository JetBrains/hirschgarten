package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.projectview.SHARD_SYNC_KEY
import org.jetbrains.bazel.languages.projectview.SectionKey
import org.jetbrains.bazel.languages.projectview.sections.presets.BooleanScalarSection

@ApiStatus.Internal
class ShardSyncSection : BooleanScalarSection() {
  override val sectionKey: SectionKey<Boolean> = SHARD_SYNC_KEY
  override val doc =
    "Directs the plugin to shard bazel build invocations when syncing " +
      "and compiling your project. Bazel builds for sync may be sharded even if " +
      "this is set to false, to keep the build command under the maximum command length (ARG_MAX)."
}
