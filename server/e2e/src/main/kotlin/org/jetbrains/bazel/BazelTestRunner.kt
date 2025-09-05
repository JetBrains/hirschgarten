package org.jetbrains.bazel

import org.jetbrains.bazel.base.BazelBaseTestRunner
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetSelector

object BazelTestRunner : BazelBaseTestRunner() {
  @JvmStatic
  fun main(args: Array<String>) {
    performTest { session ->
      val result =
        session.server.workspaceBuildTargets(WorkspaceBuildTargetParams(WorkspaceBuildTargetSelector.AllTargets))
      result.targets.toSortedMap().forEach {
        println(it.value)
      }
    }
  }
}
