package org.jetbrains.bazel.bazelrunner.outputs

fun interface OutputHandler {
  fun onNextLine(line: String)
}
