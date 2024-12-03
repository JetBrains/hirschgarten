package org.jetbrains.bsp.bazel.bazelrunner

import ch.epfl.scala.bsp4j.StatusCode
import org.jetbrains.bsp.bazel.bazelrunner.outputs.OutputCollector
import org.jetbrains.bsp.bazel.commons.BazelStatus

class BazelProcessResult(
  private val stdoutCollector: OutputCollector,
  private val stderrCollector: OutputCollector,
  private val bazelStatus: BazelStatus,
) {
  val isSuccess: Boolean get() = bspStatusCode == StatusCode.OK
  val isNotSuccess: Boolean get() = !isSuccess
  val bspStatusCode: StatusCode get() = bazelStatus.toBspStatusCode()
  val stdoutLines: List<String> get() = stdoutCollector.lines()
  val stdout: String get() = stdoutCollector.output()
  val stderrLines: List<String> get() = stderrCollector.lines()
  val stderr: String get() = stderrCollector.output()
}
