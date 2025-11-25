package org.jetbrains.bazel.sync_new.flow.diff.vfs

import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name

sealed interface SyncVFSFile {
  data class BuildFile(val path: Path) : SyncVFSFile
  data class StarlarkFile(val path: Path): SyncVFSFile
  data class WorkspaceFile(val path: Path): SyncVFSFile
  data class SourceFile(val path: Path): SyncVFSFile
}

object SyncFileClassifier {
  fun classify(path: Path): SyncVFSFile {
    val name = path.name
    val ext = path.extension
    return when {
      name == "BUILD" || name == "BUILD.bazel" -> SyncVFSFile.BuildFile(path)
      name == "MODULE" || name == "MODULE.bazel"
        || name == "WORKSPACE" || name == "WORKSPACE.bazel" -> SyncVFSFile.WorkspaceFile(path)
      ext == "bzl" -> SyncVFSFile.StarlarkFile(path)
      else -> SyncVFSFile.SourceFile(path)
    }
  }
}
