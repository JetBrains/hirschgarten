package org.jetbrains.bazel.jvm.run

import com.intellij.execution.testframework.JavaTestLocator
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import org.jetbrains.bazel.run.test.BazelTestLocatorProvider

class JavaTestLocatorProvider : BazelTestLocatorProvider {
  override fun getTestLocator(): SMTestLocator =
    JavaTestLocator.INSTANCE
}
