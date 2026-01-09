package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label
import java.nio.file.Path

sealed interface ResourceItem {
  data class File(val path: Path) : ResourceItem
  data class Target(val label: Label) : ResourceItem
}
