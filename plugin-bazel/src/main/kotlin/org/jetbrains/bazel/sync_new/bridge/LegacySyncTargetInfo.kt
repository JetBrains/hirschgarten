package org.jetbrains.bazel.sync_new.bridge

import org.jetbrains.bazel.info.BspTargetInfo
import java.nio.file.Path

data class LegacySyncTargetInfo(
  val target: BspTargetInfo.TargetInfo,
  val source: Path
)
