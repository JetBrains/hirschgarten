package org.jetbrains.bazel.hotswap

import java.nio.file.Path

data class BazelHotSwapStatus(
  val status: BazelHotSwapActionStatus,
  val changedClassCount: Int,
  val cause: Exception? = null,
)

data class BazelHotSwapFileInfo(
  val jarPath: Path,
  val modifiedClasses: List<String>,
)
