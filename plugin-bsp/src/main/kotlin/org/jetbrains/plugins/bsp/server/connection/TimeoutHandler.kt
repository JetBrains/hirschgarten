package org.jetbrains.plugins.bsp.server.connection

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration

public class TimeoutHandler(private val timeProvider: () -> Duration) {
  public constructor(constantTime: Duration) : this({ constantTime })

  private var timer = CompletableFuture<Void>()
  private var timeoutFuture = CompletableFuture<Void>().apply { complete(null) }

  public fun getUnfinishedTimeoutFuture(): CompletableFuture<Void> {
    if (timeoutFuture.isDone) {
      timeoutFuture = CompletableFuture<Void>()
      resetTimer()
    }
    return timeoutFuture
  }

  public fun resetTimer() {
    timer.cancel(true)
    timer = createTimerAndStart()
  }

  private fun createTimerAndStart(): CompletableFuture<Void> {
    return CompletableFuture<Void>()
      .completeOnTimeout(null, timeProvider().inWholeNanoseconds, TimeUnit.NANOSECONDS)
      .thenRun { timeoutFuture.completeExceptionally(TimeoutException()) }
  }
}
