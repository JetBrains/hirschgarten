package org.jetbrains.bazel.server.model

enum class Tag {
  APPLICATION,
  TEST,
  LIBRARY,
  INTELLIJ_PLUGIN,
  NO_IDE,
  NO_BUILD,
  MANUAL,
}
