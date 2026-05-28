package org.jetbrains.bazel.workspace.importer

import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import java.nio.file.Path
import kotlin.io.path.extension

internal fun String.toResolvedVirtualFileUrl(virtualFileUrlManager: VirtualFileUrlManager): VirtualFileUrl {
  val url = virtualFileUrlManager.getOrCreateFromUrl(this)
  // Add the virtual file to the VFS immediately to avoid doing it in a write action inside WorkspaceModelImpl.replaceProjectModel
  url.virtualFile
  return url
}

internal fun Path.toJarUrlString(): String =
  if (extension == "jar") {
    "jar://$this!/"
  } else {
    // There can be other library roots except for jars, e.g., Android resources. Use the file:// scheme then.
    "file://$this"
  }
