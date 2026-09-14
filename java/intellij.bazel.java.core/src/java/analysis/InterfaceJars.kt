package org.jetbrains.bazel.java.analysis

import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFile

private val INTERFACE_JAR_SUFFIXES = arrayOf("-ijar.jar", "-hjar.jar", ".abi.jar")

/**
 * Returns true if the given file is a Bazel interface jar. Bazel interface jar must have a specific suffix and be located under `bazel-out`.
 */
internal fun VirtualFile.isBazelInterfaceJar(): Boolean {
  val jar = JarFileSystem.getInstance().getVirtualFileForJar(this) ?: return false
  val name = jar.name
  return INTERFACE_JAR_SUFFIXES.any { suffix -> name.endsWith(suffix) }
         && generateSequence(jar.parent) { it.parent }.any { it.name == "bazel-out" }
}
