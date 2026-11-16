package org.jetbrains.bazel.workspace.importer

import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.invariantSeparatorsPathString

internal fun Path.toResolvedVirtualFileUrl(virtualFileUrlManager: VirtualFileUrlManager): VirtualFileUrl {
  val url = this.toVirtualFileUrl(virtualFileUrlManager)
  // Add the virtual file to the VFS immediately to avoid doing it in a write action inside WorkspaceModelImpl.replaceProjectModel
  url.virtualFile
  return url
}

internal fun String.toResolvedVirtualFileUrl(virtualFileUrlManager: VirtualFileUrlManager): VirtualFileUrl {
  val url = virtualFileUrlManager.storeAndGet(this)
  // Add the virtual file to the VFS immediately to avoid doing it in a write action inside WorkspaceModelImpl.replaceProjectModel
  url.virtualFile
  return url
}

// A URL of the IDE has forward slashes only. This matters on Windows, most of all for a UNC path like `\\wsl.localhost\Ubuntu\home`.
internal fun Path.toJarUrlString(): String =
  if (extension == "jar") {
    "jar://$invariantSeparatorsPathString!/"
  } else {
    // There can be other library roots except for jars, e.g., Android resources. Use the file:// scheme then.
    "file://$invariantSeparatorsPathString"
  }
