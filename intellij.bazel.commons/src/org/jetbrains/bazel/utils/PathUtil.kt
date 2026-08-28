package org.jetbrains.bazel.utils

import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.annotations.ApiStatus
import java.io.IOException
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.readAttributes

@ApiStatus.Internal
fun Path.allAncestorsSequence(): Sequence<Path> = generateSequence(this) { it.parent }

/**
 * See [com.intellij.openapi.vfs.VfsUtilCore.isUnder]
 */
@ApiStatus.Internal
fun Path.isUnder(ancestors: Set<Path>): Boolean = this.allAncestorsSequence().any { it in ancestors }

/**
 * Checks whether the file attributes represent a symbolic link or a Windows junction.
 *
 * On Windows, Bazel might use junctions as an alternative to convenience symbolic links.
 * They are not recognized by Java API as symlinks.
 */
@get:ApiStatus.Internal
val BasicFileAttributes.isSymbolicLinkOrJunction: Boolean get() = isSymbolicLink || isWindowsJunction

@get:ApiStatus.Internal
val BasicFileAttributes.isWindowsJunction: Boolean get() = SystemInfoRt.isWindows && isDirectory && isOther

/**
 * Checks whether the path is a Windows junction.
 *
 * This property reads the file attributes, so use [BasicFileAttributes.isWindowsJunction] when the caller
 * already holds them. The result is `false` when the attributes cannot be read.
 */
@get:ApiStatus.Internal
val Path.isWindowsJunction: Boolean get() = try {
  SystemInfoRt.isWindows && readAttributes<BasicFileAttributes>(LinkOption.NOFOLLOW_LINKS).isWindowsJunction
}
catch (_: IOException) {
  false
}

/**
 * See [com.intellij.openapi.vfs.VfsUtilCore.getCommonAncestor].
 * Input paths must be absolute and normalized.
 */
@ApiStatus.Internal
fun calculateCommonAncestor(file1: Path, file2: Path): Path? {
  if (file1 == file2) return file1

  var depth1 = file1.nameCount
  var depth2 = file2.nameCount

  var parent1: Path? = file1
  var parent2: Path? = file2
  while (depth1 > depth2 && parent1 != null) {
    parent1 = parent1.parent
    depth1--
  }
  while (depth2 > depth1 && parent2 != null) {
    parent2 = parent2.parent
    depth2--
  }
  while (parent1 != null && parent2 != null && parent1 != parent2) {
    parent1 = parent1.parent
    parent2 = parent2.parent
  }
  return parent1
}

/**
 * Input paths must be absolute and normalized.
 */
@ApiStatus.Internal
fun Collection<Path>.commonAncestor(): Path? {
  if (isEmpty()) return null
  var result: Path = first()
  for (path in asSequence().drop(1)) {
    result = calculateCommonAncestor(result, path) ?: return null
  }
  return result
}

@ApiStatus.Internal
fun Set<Path>.filterPathsThatDontContainEachOther(): List<Path> = filter { path -> !path.parent.isUnder(this) }

@ApiStatus.Internal
fun Path.refreshAndFindVirtualFile(): VirtualFile? = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(this)

@ApiStatus.Internal
fun Path.findVirtualFileLocal(): VirtualFile? = VirtualFileManager.getInstance().findFileByNioPath(this)

@ApiStatus.Internal
fun Path.findVirtualFile(): VirtualFile? = VirtualFileManager.getInstance().findFileByNioPath(this)

@ApiStatus.Internal
fun Path.findCanonicalVirtualFileThatExists(): VirtualFile? = findVirtualFile()
  ?.canonicalFile
  ?.takeIf(VirtualFile::exists)
