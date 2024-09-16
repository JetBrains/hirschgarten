package org.jetbrains.plugins.bsp.services

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.local.LocalFileSystemImpl
import java.util.concurrent.atomic.AtomicInteger

private val caseSensitiveOperations = AtomicInteger(0)

/**
 * This is a hack to skip calls to `UnixPath.toRealPath` [here](https://github.com/JetBrains/intellij-community/blob/53137070779939d6dc0176f23052668691ca0e0f/platform/platform-impl/src/com/intellij/openapi/vfs/newvfs/persistent/PersistentFSImpl.java#L590).
 * It can in theory break things if we have a non-BSP project opened side-by-side to a BSP project, or if a BSP project depends on
 * the filesystem being case-insensitive.
 * See [Slack discussion](https://jetbrains.slack.com/archives/C017MGPV4S0/p1722596236294699)
 */
suspend fun withCaseSensitiveFileSystem(block: suspend () -> Unit) {
  caseSensitiveOperations.incrementAndGet()
  try {
    block()
  } finally {
    caseSensitiveOperations.decrementAndGet()
  }
}

internal class BspCaseSensitiveFileSystem : LocalFileSystemImpl() {
  override fun toString(): String = "BspCaseSensitiveFileSystem"

  override fun getCanonicallyCasedName(file: VirtualFile): String =
    if (caseSensitiveOperations.get() > 0) {
      file.name
    } else {
      super.getCanonicallyCasedName(file)
    }
}
