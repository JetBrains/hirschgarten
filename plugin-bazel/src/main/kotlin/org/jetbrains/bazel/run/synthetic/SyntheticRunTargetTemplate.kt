package org.jetbrains.bazel.run.synthetic

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
data class SyntheticRunTargetTemplate(
  val buildFileContent: String,
  val buildFilePath: String
)
