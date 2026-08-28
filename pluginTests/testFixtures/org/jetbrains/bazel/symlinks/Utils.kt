package org.jetbrains.bazel.symlinks

import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.util.io.IoTestUtil
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

fun Path.createSymlinkOrJunction(target: Path): Path {
  when {
    SystemInfoRt.isWindows -> createJunction(target)
    IoTestUtil.isSymLinkCreationSupported -> Files.createSymbolicLink(this, target)
    else -> error("It was not possible to create any type of symlink!")
  }
  return this
}

fun Path.createJunction(target: Path): Path {
  if (!target.exists()) {
    target.createDirectories()
  }
  IoTestUtil.createJunction(target.toString(), this.toString())
  return this
}

fun Path.createBazelConvenienceSymlink(
  name: String,
  target: Path = this.resolve("execroot/$name"),
): Path = this.resolve(name).createSymlinkOrJunction(target.createDirectories())
