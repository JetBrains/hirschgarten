package org.jetbrains.bazel.sync_new.storage.util

import kotlin.text.replace

object FileUtils {
  private val SANITIZE_REGEX = Regex("[^a-zA-Z0-9_-]")

  fun sanitize(name: String): String {
    return name.replace(SANITIZE_REGEX, "_")
  }
}
