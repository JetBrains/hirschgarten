package org.jetbrains.bazel.bazelrunner

import org.jetbrains.bazel.bazelrunner.outputs.OutputCollector
import org.jetbrains.bazel.commons.BazelStatus

class BazelProcessResult(
  private val stdoutCollector: OutputCollector,
  private val stderrCollector: OutputCollector,
  val exitCode: Int,
) {
  val bazelStatus: BazelStatus
    get() = BazelStatus.fromExitCode(exitCode)

  val isSuccess: Boolean get() = bazelStatus == BazelStatus.SUCCESS
  val isNotSuccess: Boolean get() = !isSuccess

  val stdoutLines: List<String> get() = stdoutCollector.lines()
  val stdout: ByteArray get() = stdoutCollector.raw()

  val stderrLines: List<String> get() = stderrCollector.lines()
  val stderr: ByteArray get() = stderrCollector.raw()
}
