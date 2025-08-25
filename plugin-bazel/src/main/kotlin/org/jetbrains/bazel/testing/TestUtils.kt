package org.jetbrains.bazel.testing

const val IS_IN_IDE_STARTER_TEST = "is.in.ide.starter.test"

object TestUtils {
  fun isInIdeStarterTest(): Boolean = System.getProperty(IS_IN_IDE_STARTER_TEST).toBoolean()
}
