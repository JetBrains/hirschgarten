package org.jetbrains.bazel.languages.projectview.language

import org.intellij.lang.annotations.Language
import org.jetbrains.bazel.utils.refreshAndFindVirtualFile
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes

/** Writes UTF-8 text under this root and lets VFS see the file. */
internal fun Path.writeProjectViewFile(relativePath: String, @Language("projectview") content: String): Path =
  writeUnderRoot(relativePath, content).also { it.refreshAndFindVirtualFile() }

/**
 * Writes a file and deliberately does not let VFS see it.
 *
 * This is the situation of `BazelProjectStorePathCustomizer`, which parses the project view before the project exists,
 * and of an imported file that nothing has opened yet.
 */
internal fun Path.writeProjectViewFileUnknownToVfs(relativePath: String, @Language("projectview") content: String): Path =
  writeUnderRoot(relativePath, content)

private fun Path.writeUnderRoot(relativePath: String, content: String): Path {
  val path = resolve(relativePath)
  path.parent.createDirectories()
  path.writeBytes(content.toByteArray(Charsets.UTF_8))
  return path
}
