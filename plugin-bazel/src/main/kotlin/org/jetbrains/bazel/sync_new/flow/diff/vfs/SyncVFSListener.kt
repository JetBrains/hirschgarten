package org.jetbrains.bazel.sync_new.flow.diff.vfs

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.isFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.BulkFileListenerBackgroundable
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.util.messages.MessageBusConnection
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.collections.forEach

class SyncVFSListener(
  private val project: Project,
  private val disposable: Disposable
) {
  // TODO: too many changes threshold
  // TODO: should I also persist this map???
  val fileStates: ConcurrentMap<Path, SyncFileState> = ConcurrentHashMap()

  private var attached: Boolean = false
  private var connection: MessageBusConnection? = null

  internal fun ensureAttached() {
    synchronized(this) {
      if (attached) {
        return
      }
      val connection = project.messageBus.connect()
      connection.subscribe(
        VirtualFileManager.VFS_CHANGES_BG,
        object : BulkFileListenerBackgroundable {
          override fun after(events: List<VFileEvent>) {
            events.forEach { processVFSEvent(it) }
          }
        },
      )
      Disposer.register(disposable, connection)
      this.connection = connection
      attached = true
    }
  }

  internal fun ensureDisconnected() {
    synchronized(this) {
      if (!attached) {
        return
      }
      attached = false
      this.connection?.disconnect()
    }
  }

  // TODO: better filtering
  private fun processVFSEvent(event: VFileEvent) {
    val file = event.file ?: return
    when (event) {
      is VFileCreateEvent -> {
        file.toNioPathOrNull()?.let { fileStates[it] = SyncFileState.ADDED }
      }

      is VFileDeleteEvent -> {
        file.toNioPathOrNull()?.let { fileStates[it] = SyncFileState.REMOVED }
      }

      is VFileContentChangeEvent, is VFilePropertyChangeEvent -> {
        file.toNioPathOrNull()?.let { fileStates[it] = SyncFileState.CHANGED }
      }

      is VFileCopyEvent -> {
        val newParent = event.newParent.toNioPathOrNull()
        if (newParent != null) {
          val newFile = newParent.resolve(event.newChildName)
          fileStates.compute(newFile) { _, state ->
            when {
              state == SyncFileState.ADDED -> SyncFileState.CHANGED
              else -> SyncFileState.ADDED
            }
          }
        }
      }

      is VFileMoveEvent -> {
        val newParent = event.newParent.toNioPathOrNull()
        val oldParent = event.oldParent.toNioPathOrNull()
        if (newParent != null && oldParent != null) {
          fileStates[Path.of(event.oldPath)] = SyncFileState.REMOVED
          fileStates[Path.of(event.newPath)] = SyncFileState.ADDED
        }
      }
    }
  }

  //private fun processVFSEvent(event: VFileEvent) {
  //  if (!event.isSupportedEvent()) {
  //    return
  //  }
  //  val file = event.file ?: return
  //  if (!file.isFile) {
  //    return
  //  }
  //  if (!file.isBazelFile()) {
  //    return
  //  }
  //  changed.add(file)
  //}
  //
  //private fun VirtualFile.isBazelFile() = when {
  //  name == "BUILD"
  //    || name == "BUILD.bazel" -> true
  //
  //  extension == "bzl" -> true
  //  else -> false
  //}
  //
  //private fun VFileEvent.isSupportedEvent() = when (this) {
  //  is VFileCreateEvent -> true
  //  is VFileDeleteEvent -> true
  //  is VFileContentChangeEvent -> true
  //  else -> false
  //}
}
