package org.jetbrains.bazel.fastbuild

import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Path

data class FastBuildStatus(val workspaceRoot: Path, var status: FastBuildActionStatus? = null)

data class FastBuildTargetStatus(
  val inputFile: VirtualFile,
  val targetJar: Path,
  var status: FastBuildActionStatus,
)
