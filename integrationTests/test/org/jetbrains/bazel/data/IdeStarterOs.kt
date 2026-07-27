package org.jetbrains.bazel.data

import java.util.Locale

internal const val USE_BAZEL_VERSION_ENV = "USE_BAZEL_VERSION"

enum class IdeStarterOs(val id: String) {
  LINUX("linux"),
  MACOS("macos"),
  WINDOWS("windows"),
  ;

  companion object {
    fun current(osName: String = System.getProperty("os.name")): IdeStarterOs {
      val normalized = osName.lowercase(Locale.US)
      return when {
        normalized.startsWith("windows") -> WINDOWS
        normalized.startsWith("mac") || normalized.startsWith("darwin") -> MACOS
        normalized.startsWith("linux") -> LINUX
        else -> error("Unsupported IDE-Starter host OS '$osName'")
      }
    }
  }
}
