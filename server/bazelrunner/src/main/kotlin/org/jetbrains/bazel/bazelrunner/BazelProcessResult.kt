package org.jetbrains.bazel.bazelrunner

import ch.epfl.scala.bsp4j.StatusCode
import org.jetbrains.bazel.bazelrunner.outputs.OutputCollector
import org.jetbrains.bazel.commons.BazelStatus

class BazelProcessResult(
  private val stdoutCollector: OutputCollector,
  private val stderrCollector: OutputCollector,
  val bazelStatus: BazelStatus,
) {
  val isSuccess: Boolean get() = bspStatusCode == StatusCode.OK
  val isNotSuccess: Boolean get() = !isSuccess
  val bspStatusCode: StatusCode get() = bazelStatus.toBspStatusCode()
  val stdoutLines: List<String> get() = stdoutCollector.lines()
  val stdout: String get() = stdoutCollector.output()
  val stderrLines: List<String> get() = stderrCollector.lines()
  val stderr: String get() = stderrCollector.output()
}
