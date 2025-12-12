package org.jetbrains.bazel.sync_new.flow.vfs_diff

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFileManager
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
import org.jetbrains.bazel.sync_new.codec.kryo.ofKryo
import org.jetbrains.bazel.sync_new.codec.ofPath
import org.jetbrains.bazel.sync_new.storage.KVStore
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.jetbrains.bazel.sync_new.storage.createKVStore
import org.jetbrains.bazel.sync_new.storage.set
import org.jetbrains.bazel.sync_new.storage.storageContext
import java.nio.file.Path
import kotlin.collections.forEach

class SyncVFSListener(
  private val project: Project,
  private val disposable: Disposable,
) {
  // TODO: rules for accepting file exensions for file watcher
  // TODO: too many changes threshold
  // TODO: should I also persist this map???
  val file2State: KVStore<Path, SyncFileState> =
    project.storageContext.createKVStore<Path, SyncFileState>("bazel.sync.vfs_diff.file2State", StorageHints.USE_IN_MEMORY)
      .withKeyCodec { ofPath() }
      .withValueCodec { ofKryo() }
      .build()

  private var attached: Boolean = false
  private var connection: MessageBusConnection? = null
  private val filter = SyncVFSFileFilter(project)

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
    if (!filter.isWatchableFile(file)) {
      return
    }
    when (event) {
      is VFileCreateEvent -> {
        file.toNioPathOrNull()?.let { file2State[it] = SyncFileState.ADDED }
      }

      is VFileDeleteEvent -> {
        file.toNioPathOrNull()?.let { file2State[it] = SyncFileState.REMOVED }
      }

      is VFileContentChangeEvent, is VFilePropertyChangeEvent -> {
        file.toNioPathOrNull()?.let { file2State.computeIfAbsent(it) { _ -> SyncFileState.CHANGED } }
      }

      is VFileCopyEvent -> {
        val newParent = event.newParent.toNioPathOrNull()
        if (newParent != null) {
          val newFile = newParent.resolve(event.newChildName)
          file2State.compute(newFile) { _, state ->
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
          file2State[Path.of(event.oldPath)] = SyncFileState.REMOVED
          file2State[Path.of(event.newPath)] = SyncFileState.ADDED
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
