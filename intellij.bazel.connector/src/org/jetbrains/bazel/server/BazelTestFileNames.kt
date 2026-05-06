package org.jetbrains.bazel.server

internal enum class BazelTestFileNames(val filename: String) {
   LOG("test.log"),
   COVERAGE("test.lcov"),
   XML("test.xml");

  val filenameRegex: String
    get() = ".*/" +  filename.replace("\\", "\\\\").replace(".", "\\.")
}
