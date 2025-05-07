package org.jetbrains.bazel.cpp.sync.compiler

// See com.google.idea.blaze.cpp.CompilerVersionUtil
object CompilerVersionUtil {
  fun isClang(version: String): Boolean = version.contains("clang")

  fun isAppleClang(version: String): Boolean = version.contains("Apple") && isClang(version)

  fun isMSVC(version: String): Boolean = version.contains("Microsoft")
}
