package org.jetbrains.bazel

import org.jetbrains.bazel.base.BazelBaseTestRunner

object BazelTestRunner : BazelBaseTestRunner() {

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-95
  @JvmStatic
  fun main(args: Array<String>) {
    performTest()
  }
}

