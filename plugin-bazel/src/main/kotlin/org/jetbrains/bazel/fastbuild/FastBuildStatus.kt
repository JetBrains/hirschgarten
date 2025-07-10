package org.jetbrains.bazel.fastbuild

import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Path

data class FastBuildStatus(
  val workspaceRoot: Path,
  var targets: List<FastBuildTargetStatus>? = null,
  var status: String? = null,
)

data class FastBuildTargetStatus(
  val inputFile: VirtualFile,
  val targetJar: Path,
  var compilerStatus: String,
)
