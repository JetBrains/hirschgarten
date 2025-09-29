package org.jetbrains.bazel.sdkcompat

import kotlinx.coroutines.Deferred

interface HasDeferredPid {
  val pid: Deferred<Long?>?
}
