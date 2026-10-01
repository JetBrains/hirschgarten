package org.jetbrains.bazel.assertions

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS
import org.jetbrains.bazel.sync.environment.projectCtx
import org.junit.jupiter.api.fail
import java.nio.file.Path
import kotlin.io.path.Path

private fun getChildrenInVfs(dir: VirtualFile): Sequence<Path> = sequence {
  val persistentFS = PersistentFS.getInstance()
  if (!persistentFS.wereChildrenAccessed(dir)) return@sequence

  for (name in persistentFS.listPersisted(dir)) {
    val child = dir.findChild(name) ?: continue

    if (child.isDirectory) {
      yieldAll(getChildrenInVfs(child))
    } else {
      yield(Path.of(child.path))
    }
  }
}

internal fun Project.assertVfsLoads() {
  val executionRoot = requireNotNull(projectCtx.bazelExecPath)
  val executionRootFile = VfsUtil.findFile(executionRoot, /* refreshIfNeeded = */ false) ?: return

  for (child in getChildrenInVfs(executionRootFile)) {
    val relativePath = executionRoot.relativize(child)
    if (relativePath.startsWith(Path("external"))) {
      // Things downloaded into external/ almost never change, so adding it as a VFS root directly won't cause too many events anyway.
      continue
    }

    fail { "$child is not in allowed VFS, debug with: '-Dfile.system.trace.loading=$child'" }
  }
}

