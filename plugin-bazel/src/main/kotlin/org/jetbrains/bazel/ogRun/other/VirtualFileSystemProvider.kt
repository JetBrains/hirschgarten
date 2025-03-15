package org.jetbrains.bazel.ogRun.other

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.LocalFileSystem

/** Provides indirection for virtual file systems.  */
interface VirtualFileSystemProvider {
  val system: LocalFileSystem

  companion object {
    val instance: VirtualFileSystemProvider
      get() =
        ApplicationManager.getApplication().getService(
          VirtualFileSystemProvider::class.java,
        )
  }
}
