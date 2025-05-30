package org.jetbrains.bazel.server.model

enum class Tag {
  APPLICATION,
  TEST,
  LIBRARY,
  INTELLIJ_PLUGIN,
  NO_IDE,
  NO_BUILD,
  MANUAL,
  LIBRARIES_OVER_MODULES, // used with the setting `experimental_prioritize_libraries_over_modules_target_kinds`

  /**
   * If a target has this tag, then if it shares sources with another target, then we should prioritize the other target instead
   * when resolving the conflict.
   */
  IDE_LOW_SHARED_SOURCES_PRIORITY,
}
