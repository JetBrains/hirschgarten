package org.jetbrains.bazel.workspace.fileEvents

import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import org.jetbrains.bazel.utils.SourceType
import org.jetbrains.bazel.utils.isSourceFile
import java.nio.file.Path
import kotlin.io.path.extension

internal sealed class SimplifiedFileEvent private constructor(
  val fileRemoved: Path?,
  val fileAdded: Path?,
  val newVirtualFile: VirtualFile? = null,
) {
  private constructor(fileRemoved: String? = null, fileAdded: String? = null, newVirtualFile: VirtualFile? = null) : this(
    fileRemoved = fileRemoved?.toNioPathOrNull(),
    fileAdded = fileAdded?.toNioPathOrNull(),
    newVirtualFile = newVirtualFile,
  )

  // either of the affected files is a source file
  fun shouldBeProcessed(): Boolean =
    fileRemoved?.extension?.let { SourceType.fromExtension(it) } != null || fileAdded?.isSourceFile() == true

  fun doesAffectFolder(folderPath: Path): Boolean =
    fileRemoved?.startsWith(folderPath) == true || fileAdded?.startsWith(folderPath) == true

  companion object {
    /** @return `SimplifiedFileEvent` if it should be processed, `null` otherwise */
    fun from(event: VFileEvent): SimplifiedFileEvent? =
      when (event) {
        is VFileCreateEvent -> Create(event)
        is VFileCopyEvent -> Copy(event)
        is VFileDeleteEvent -> Delete(event)
        is VFileMoveEvent -> Move(event)
        is VFilePropertyChangeEvent -> {
          if (event.propertyName == VirtualFile.PROP_NAME) { // file rename
            Rename(event)
          } else { // property change other than file rename
            null
          }
        }
        else -> null
      }?.takeIf { it.shouldBeProcessed() }
  }
  class Create(originalEvent: VFileCreateEvent)
    : SimplifiedFileEvent(fileAdded = originalEvent.path, newVirtualFile = originalEvent.file)

  class Copy(originalEvent: VFileCopyEvent)
    : SimplifiedFileEvent(fileAdded = originalEvent.path, newVirtualFile = originalEvent.findCreatedFile())

  class Delete(originalEvent: VFileDeleteEvent) : SimplifiedFileEvent(fileRemoved = originalEvent.path)

  class Move(originalEvent: VFileMoveEvent)
    : SimplifiedFileEvent(fileRemoved = originalEvent.oldPath, fileAdded = originalEvent.newPath, newVirtualFile = originalEvent.file)

  class Rename(originalEvent: VFilePropertyChangeEvent)
    : SimplifiedFileEvent(fileRemoved = originalEvent.oldPath, fileAdded = originalEvent.newPath, newVirtualFile = originalEvent.file) {
    val extensionChanged = fileRemoved?.extension.orEmpty() != fileAdded?.extension.orEmpty()
  }
}
