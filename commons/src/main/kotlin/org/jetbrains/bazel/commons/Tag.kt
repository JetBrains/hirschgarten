package org.jetbrains.bazel.commons

enum class Tag {
  APPLICATION,
  TEST,
  LIBRARY,
  INTELLIJ_PLUGIN,
  NO_IDE,
  NO_BUILD,
  MANUAL,

  /**
   * If a target has this tag, then if it shares sources with another target, then we should prioritize the other target instead
   * when resolving the conflict.
   */
  IDE_LOW_SHARED_SOURCES_PRIORITY,
}
