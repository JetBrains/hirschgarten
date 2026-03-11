package org.jetbrains.bazel.progress

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
enum class ShowConsole {
  ALWAYS,
  ON_FAIL,
  NEVER,
}
