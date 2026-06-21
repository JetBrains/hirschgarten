package org.jetbrains.bazel.server.sync

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import com.intellij.aspect.lib.readTargetFromFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.sync.workspace.snapshot.toWorkspaceTargetKey
import org.jetbrains.bsp.protocol.BazelTaskLogger
import java.nio.file.Path

internal class TargetInfoReader(private val taskLogger: BazelTaskLogger?) {
  suspend fun readTargetMapFromAspectOutputs(files: Set<Path>): Map<WorkspaceTargetKey, TargetIdeInfo> =
    withContext(Dispatchers.Default) {
      files.map { file ->
        async {
          readTargetFromFile(file) { msg -> taskLogger?.error("Could not read target info $file: ${msg}") }
        }
      }.awaitAll()
    }.filterNotNull().associateBy { it.key.toWorkspaceTargetKey() }
}
