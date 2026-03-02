package org.jetbrains.bazel.commons

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
enum class Tag {
  APPLICATION,
  TEST,
  LIBRARY,
  INTELLIJ_PLUGIN,
  NO_IDE,
  NO_BUILD,
  MANUAL,
}
