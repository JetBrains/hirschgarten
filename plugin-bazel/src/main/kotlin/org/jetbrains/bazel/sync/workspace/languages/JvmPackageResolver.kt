package org.jetbrains.bazel.sync.workspace.languages

import java.nio.file.Path

/**
 * Interface for resolving JVM package prefixes from source files.
 * This abstraction allows for testing without file system access.
 */
interface JvmPackageResolver {
  /**
   * Calculates the JVM package prefix for a given source file.
   *
   * @param source The path to the source file
   * @param multipleLines Whether to check multiple lines for package declarations (used for Scala)
   * @return The package prefix, or null if not found
   */
  fun calculateJvmPackagePrefix(source: Path, multipleLines: Boolean = false): String?
}

/**
 * Default implementation that reads from the file system.
 */
class DefaultJvmPackageResolver : JvmPackageResolver {
  override fun calculateJvmPackagePrefix(source: Path, multipleLines: Boolean): String? =
    JVMLanguagePluginParser.calculateJVMSourceRootAndAdditionalData(source, multipleLines)
}
