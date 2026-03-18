package org.jetbrains.bazel.testing

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
const val IS_IN_IDE_STARTER_TEST = "is.in.ide.starter.test"

@ApiStatus.Internal
object TestUtils {
  fun isInIdeStarterTest(): Boolean = System.getProperty(IS_IN_IDE_STARTER_TEST).toBoolean()
}
