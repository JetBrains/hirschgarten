package org.jetbrains.bazel

import org.jetbrains.bazel.base.BazelBaseTestRunner
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetSelector
import kotlin.time.Duration.Companion.seconds

object BazelTestRunner : BazelBaseTestRunner() {

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-95
  @JvmStatic
  fun main(args: Array<String>) {
    createTestkitClient().test(60.seconds) { session ->
      val result =
        session.server.workspaceBuildTargets(WorkspaceBuildTargetParams(WorkspaceBuildTargetSelector.AllTargets))
      result.targets.toSortedMap().forEach {
        println(it.value)
      }
    }
  }
}

