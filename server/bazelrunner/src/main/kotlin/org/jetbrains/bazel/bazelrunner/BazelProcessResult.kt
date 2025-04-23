package org.jetbrains.bazel.bazelrunner

import org.jetbrains.bazel.bazelrunner.outputs.OutputCollector
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bsp.protocol.StatusCode

class BazelProcessResult(
  private val stdoutCollector: OutputCollector,
  private val stderrCollector: OutputCollector,
  val bazelStatus: BazelStatus,
) {
  val isSuccess: Boolean get() = bspStatusCode == StatusCode.OK
  val isNotSuccess: Boolean get() = !isSuccess
  val bspStatusCode: StatusCode get() = bazelStatus.toBspStatusCode()
  val stdoutLines: List<String> get() = stdoutCollector.lines()

  /**
   * Don't use [stdout] directly to parse JSON or XML, because it can contain additional informational prints
   * (see [YT issue](https://youtrack.jetbrains.com/issue/BAZEL-1930)).
   * Use [org.jetbrains.bazel.server.bsp.utils.toJson] or [org.jetbrains.bazel.server.bsp.utils.readXML] instead.
   */
  val stdout: String get() = stdoutCollector.output()
  val stderrLines: List<String> get() = stderrCollector.lines()
  val stderr: String get() = stderrCollector.output()
}
