package org.jetbrains.bazel.sdkcompat

import com.intellij.testFramework.IndexingTestUtil

// IndexingTestUtil.forceSkipWaiting doesn't exist in 251
fun indexingTestUtilForceSkipWaiting() {
  IndexingTestUtil.forceSkipWaiting = true
}
