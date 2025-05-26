package org.jetbrains.bazel.utils

object BazelWorkingDirectoryManager {
  @Volatile
  private var workingDirectory: String = ""

  fun getWorkingDirectory(): String = workingDirectory

  fun setWorkingDirectory(directory: String) {
    workingDirectory = directory
  }
}
