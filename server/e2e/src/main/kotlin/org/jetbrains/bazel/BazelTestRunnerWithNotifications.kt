package org.jetbrains.bazel

import org.jetbrains.bazel.base.BazelBaseTestRunner
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.CompileParams
import kotlin.time.Duration.Companion.seconds

object BazelTestRunnerWithNotifications : BazelBaseTestRunner() {

  private const val COMPILE_TARGETS = "COMPILE_TARGETS_"

  private fun getSteps(): List<List<Label>> =
    System.getenv().filter { it.key.startsWith(COMPILE_TARGETS) }.toSortedMap().map {
      it.value.split(",").map { target -> Label.parse(target) }
    }

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-95
  @JvmStatic
  fun main(args: Array<String>) {
    val testClient = createTestkitClient()
    getSteps().forEach { compileTargets ->
      testClient.test(360.seconds) { session ->
        val params =
          CompileParams(
            compileTargets,
            originId = "some-id",
            arguments = listOf(
              "--action_env=FORCE_REBUILD=${System.currentTimeMillis()}",
            ),
          )
        session.client.clearDiagnostics()
        session.server.buildTargetCompile(params)
        session.client.publishDiagnosticsNotifications.sortedBy { it.buildTarget }.filter { it.textDocument != null }.forEach {
          println(it)
        }
      }
    }
  }
}

