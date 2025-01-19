package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.nio.file.Path

internal fun Path.toResolvedVirtualFileUrl(virtualFileUrlManager: VirtualFileUrlManager): VirtualFileUrl {
  val url = this.toVirtualFileUrl(virtualFileUrlManager)
  // Add the virtual file to the VFS immediately to avoid doing it in a write action inside WorkspaceModelImpl.replaceProjectModel
  url.virtualFile
  return url
}

internal fun String.toResolvedVirtualFileUrl(virtualFileUrlManager: VirtualFileUrlManager): VirtualFileUrl {
  val url = virtualFileUrlManager.getOrCreateFromUrl(this)
  // Add the virtual file to the VFS immediately to avoid doing it in a write action inside WorkspaceModelImpl.replaceProjectModel
  url.virtualFile
  return url
}

suspend fun List<Path>.toResolvedVirtualFileUrls(virtualFileUrlManager: VirtualFileUrlManager): List<VirtualFileUrl> =
  withContext(Dispatchers.IO) {
    map { async { it.toResolvedVirtualFileUrl(virtualFileUrlManager) } }.awaitAll()
  }
