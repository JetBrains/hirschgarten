package org.jetbrains.bazel.golang.debug

import com.intellij.execution.process.NopProcessHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Guards [delveAttachStopCondition]: the Bazel Go debugger must attach with a
 * process-bound, unbounded retry when a Delve process handler is present (so a slow Delve that opens its
 * listen port after the default ~19s window still attaches), and fall back to the bounded window otherwise.
 * Reverting the fix drops the stop condition and fails the first test.
 */
class BazelGoDebugRunnerTest {
  @Test
  fun `attach retries until the process exits when a Delve process handler is available`() {
    val delve = NopProcessHandler().apply { startNotify() }

    val stopCondition = delveAttachStopCondition(delve)

    assertThat(stopCondition)
      .describedAs("a non-null stop condition makes RemoteVmConnection.doOpen retry unbounded (maxAttemptCount = -1)")
      .isNotNull()
    assertThat(stopCondition!!.value(null))
      .describedAs("while the Delve process is alive the attach must keep retrying")
      .isFalse()

    delve.destroyProcess()
    assertThat(stopCondition.value(null))
      .describedAs("once the Delve process is gone the attach must stop retrying")
      .isTrue()
  }

  @Test
  fun `attach falls back to the bounded window when no process handler is available`() {
    assertThat(delveAttachStopCondition(null))
      .describedAs("a null stop condition keeps RemoteVmConnection.doOpen on the bounded DEFAULT_CONNECT_ATTEMPT_COUNT window")
      .isNull()
  }
}
