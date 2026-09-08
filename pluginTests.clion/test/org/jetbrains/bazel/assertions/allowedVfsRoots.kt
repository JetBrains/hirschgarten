package org.jetbrains.bazel.assertions

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.assertions.AllowedVfsRoot.Configuration
import java.nio.file.Path

data class AllowedVfsRoot(
  val configuration: Configuration,
  val path: Path,
  val recursive: Boolean,
) {

  enum class Configuration { ANY, FASTBUILD, DEBUG }

  companion object {

    fun flat(path: String, configuration: Configuration = Configuration.FASTBUILD): AllowedVfsRoot = AllowedVfsRoot(
      configuration = configuration,
      path = Path.of(path),
      recursive = false,
    )

    fun recursive(path: String, configuration: Configuration = Configuration.FASTBUILD): AllowedVfsRoot = AllowedVfsRoot(
      configuration = configuration,
      path = Path.of(path),
      recursive = true,
    )
  }

  override fun toString(): String = buildString {
    append("[$configuration]: ")
    if (recursive) append('|')
    append(path.toString())
  }
}

private fun matches(root: AllowedVfsRoot, path: Path): Boolean {
  require(!path.isAbsolute) { "the path should relative to the execution root" }
  require(path.nameCount > 3) { "the path should contain more then three segments" }
  require(path.getName(0).toString() == "bazel-out") { "the path should start with bazel-out" }
  require(path.getName(2).toString() == "bin") { "the path should reside in bazel-bin" }

  val actualConfiguration = path.getName(1).toString()
  if (root.configuration == Configuration.FASTBUILD && !actualConfiguration.contains("fastbuild")) return false
  if (root.configuration == Configuration.DEBUG && !actualConfiguration.contains("dbg")) return false

  val actualPath = path.subpath(3, path.nameCount)
  return if (root.recursive) {
    actualPath.startsWith(root.path)
  }
  else {
    root.path == actualPath.parent
  }
}
private fun getChildrenInVfs(dir: VirtualFile): Sequence<Path> = sequence {
  val persistentFS = PersistentFS.getInstance()
  if (!persistentFS.wereChildrenAccessed(dir)) return@sequence

  for (name in persistentFS.listPersisted(dir)) {
    val child = dir.findChild(name) ?: continue

    if (child.isDirectory) {
      yieldAll(getChildrenInVfs(child))
    } else {
      yield(Path.of(child.path))
    }
  }
}

internal fun assertVfsLoads(executionRoot: Path, allowedRoots: List<AllowedVfsRoot>) {
  val root = VfsUtil.findFile(executionRoot, /* refreshIfNeeded = */ false) ?: return

  for (child in getChildrenInVfs(root)) {
    assertThat(allowedRoots.any { matches(it, executionRoot.relativize(child)) }).withFailMessage {
      val roots = allowedRoots.joinToString(";")
      "$child is not in allowed roots: [$roots], debug with: '-Dfile.system.trace.loading=$child'"
    }.isTrue()
  }
}

