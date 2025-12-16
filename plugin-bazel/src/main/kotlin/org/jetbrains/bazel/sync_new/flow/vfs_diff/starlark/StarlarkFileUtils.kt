package org.jetbrains.bazel.sync_new.flow.vfs_diff.starlark

import com.dynatrace.hash4j.hashing.HashValue128
import com.google.devtools.build.lib.query2.proto.proto2api.Build
import org.jetbrains.bazel.sync_new.storage.hash.hash
import java.nio.file.Path
import kotlin.io.path.absolutePathString

fun Build.SourceFile.toPath(): Path =
  if (location.contains(':')) {
    Path.of(location.substringBefore(':'))
  } else {
    Path.of(location)
  }

object StarlarkFileUtils {
  fun hashWorkspacePath(path: Path): HashValue128 = hash { putString(path.absolutePathString()) }
}
