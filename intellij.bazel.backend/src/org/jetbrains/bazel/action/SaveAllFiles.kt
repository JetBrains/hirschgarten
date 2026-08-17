package org.jetbrains.bazel.action

import com.intellij.openapi.application.EDT
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.newvfs.ManagingFS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
suspend fun saveAllFiles() {
  withContext(Dispatchers.EDT) {
    FileDocumentManager.getInstance().saveAllDocuments()
  }
  // Flush VFS before calling Bazel: https://blog.jetbrains.com/platform/2026/06/async-vfs-content-writes-what-plugin-authors-need-to-know/
  ManagingFS.getInstance().flushPendingUpdatesOrNotify()
}
