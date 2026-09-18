package org.jetbrains.bazel.clion.debug

import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.ManagingFS
import com.intellij.openapi.vfs.newvfs.NewVirtualFile
import org.jetbrains.bazel.sync.environment.projectCtx

private const val NO_EXTENSION = "<none>"

/** Reports every execution-root file that the VFS holds, plus a file-extension histogram. */
internal class DebugExcrootVFSAction : BazelDebugAction() {

  override suspend fun exec(project: Project): Any {
    val root = project.projectCtx.bazelExecPath ?: fail("no execution root found")

    return readAction {
      val virtualRoot = VirtualFileManager.getInstance().findFileByNioPath(root)
        ?: fail("no virtual file found for the execution root")

      val files = mutableListOf<String>()
      val histogram = mutableMapOf<String, Long>()

      for (child in persistedFilesUnder(virtualRoot)) {
        val extension = child.extension ?: NO_EXTENSION
        histogram[extension] = histogram.getOrDefault(extension, 0L) + 1L
        files += child.path
      }

      mapOf(
        "execution_root" to root.toString(),
        "file_count" to files.size,
        "histogram" to histogram.entries
          .sortedByDescending { it.value }
          .associate { it.key to it.value },
        "files" to files,
      )
    }
  }
}

/**
 * Walks the children that the VFS already holds.
 *
 * The walk never loads a directory from the file system, so the output shows
 * the VFS state alone.
 */
private fun persistedFilesUnder(dir: VirtualFile): Sequence<VirtualFile> =
  sequence {
    if (dir !is NewVirtualFile) return@sequence
    if (!ManagingFS.getInstance().wereChildrenAccessed(dir)) return@sequence

    for (child in dir.iterInDbChildren()) {
      if (child.isDirectory) {
        yieldAll(persistedFilesUnder(child))
      }
      else {
        yield(child)
      }
    }
  }
