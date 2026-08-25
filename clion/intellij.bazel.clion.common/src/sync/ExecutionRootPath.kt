package org.jetbrains.bazel.clion.sync

import java.nio.file.Path

/**
 * Marker class for paths that are either absolute (non-hermetic) or relative
 * to the execution root.
 */
@JvmInline
value class ExecutionRootPath(val path: Path) {

  constructor(path: String) : this(Path.of(path))
}
