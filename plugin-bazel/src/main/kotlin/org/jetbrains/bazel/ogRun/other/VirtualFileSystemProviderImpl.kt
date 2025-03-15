package org.jetbrains.bazel.ogRun.other

import com.intellij.openapi.vfs.LocalFileSystem

/** Default implementation of [VirtualFileSystemProvider].  */
class VirtualFileSystemProviderImpl : VirtualFileSystemProvider {
  override val system: LocalFileSystem
    get() = LocalFileSystem.getInstance()
}
