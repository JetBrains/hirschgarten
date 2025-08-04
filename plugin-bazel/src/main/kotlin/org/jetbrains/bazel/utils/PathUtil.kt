package org.jetbrains.bazel.utils

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Path

fun Path.allAncestorsSequence(): Sequence<Path> = generateSequence(this) { it.parent }

/**
 * See [com.intellij.openapi.vfs.VfsUtilCore.isUnder]
 */
fun Path.isUnder(ancestors: Set<Path>): Boolean = this.allAncestorsSequence().any { it in ancestors }

/**
 * See [com.intellij.openapi.vfs.VfsUtilCore.getCommonAncestor].
 * Input paths must be absolute and normalized.
 */
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
fun Collection<Path>.commonAncestor(): Path? {
  if (isEmpty()) return null
  var result: Path = first()
  for (path in asSequence().drop(1)) {
    result = calculateCommonAncestor(result, path) ?: return null
  }
  return result
}

fun Set<Path>.filterPathsThatDontContainEachOther(): List<Path> = filter { path -> !path.parent.isUnder(this) }

fun Path.refreshAndFindVirtualFile(): VirtualFile? = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(this)

fun Path.findVirtualFile(): VirtualFile? = LocalFileSystem.getInstance().findFileByNioFile(this)
