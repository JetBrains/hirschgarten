package org.jetbrains.bazel.cpp.sync

// See com.google.idea.blaze.cpp.CFileExtensions
internal object CFileExtensions {
  // See https://bazel.build/versions/master/docs/be/c-cpp.html#cc_binary.srcs
  val C_FILE_EXTENSIONS: Set<String> = setOf("c")
  val CXX_FILE_EXTENSIONS: Set<String> = setOf("cc", "cpp", "cxx", "c++", "C")

  val CXX_ONLY_HEADER_EXTENSIONS: Set<String> = setOf("hh", "hpp", "hxx")
  private val SHARED_HEADER_EXTENSIONS: Set<String> = setOf("h", "inc")

  val SOURCE_EXTENSIONS: Set<String> = C_FILE_EXTENSIONS + CXX_FILE_EXTENSIONS

  val HEADER_EXTENSIONS: Set<String> = SHARED_HEADER_EXTENSIONS + CXX_ONLY_HEADER_EXTENSIONS
}
