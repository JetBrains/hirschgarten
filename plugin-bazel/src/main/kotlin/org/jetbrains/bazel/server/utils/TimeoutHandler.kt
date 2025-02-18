package org.jetbrains.bazel.server.utils

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration

class TimeoutHandler(private val timeProvider: () -> Duration) {
  private var timer = CompletableFuture<Void>()
  private var timeoutFuture = CompletableFuture<Void>().apply { complete(null) }

  fun getUnfinishedTimeoutFuture(): CompletableFuture<Void> {
    if (timeoutFuture.isDone) {
      timeoutFuture = CompletableFuture<Void>()
      resetTimer()
    }
    return timeoutFuture
  }

  fun resetTimer() {
    timer.cancel(true)
    timer = createTimerAndStart()
  }

  private fun createTimerAndStart(): CompletableFuture<Void> =
    CompletableFuture<Void>()
      .completeOnTimeout(null, timeProvider().inWholeNanoseconds, TimeUnit.NANOSECONDS)
      .thenRun { timeoutFuture.completeExceptionally(TimeoutException()) }
}
