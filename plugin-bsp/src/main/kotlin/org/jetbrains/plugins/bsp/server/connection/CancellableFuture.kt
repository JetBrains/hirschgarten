package org.jetbrains.plugins.bsp.server.connection

import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture

/**
 * Some BSP endpoints return `CompletableFuture`s that represent BSP jobs that are
 * currently running on BSP server. In order to cancel these, jobs, the `cancel`
 * method of this future should be called. Unfortunately, the original futures that come
 * from lsp4j does not support transformations well - after calling `thenApply`, the `cancel`
 * method of the new future does not stop the job.
 *
 * @see <a href="https://github.com/JetBrains/intellij-scala/commit/ee5bf9e296b06fdedaae316dc63e6782c35e0f00">more details</a>
 */
public class CancellableFuture<T> private constructor(private val original: CompletableFuture<*>) :
  CompletableFuture<T>() {

  override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
    original.cancel(mayInterruptIfRunning)
    return super.cancel(mayInterruptIfRunning)
  }

  override fun <U> newIncompleteFuture(): CompletableFuture<U> = CancellableFuture(original)

  public companion object {
    public fun <T> from(original: CompletableFuture<T>): CancellableFuture<T> {
      val result = CancellableFuture<T>(original)
      original.whenComplete { value, error ->
        when (error) {
          null -> result.complete(value)
          else -> result.completeExceptionally(error)
        }
      }

      return result
    }
  }
}

/**
 * Cancels this future when given future is cancelled.
 * Completes this future exceptionally when given future completes exceptionally (with the same exception).
 *
 * @param T type of this future
 * @param otherFuture future, to whose completion this method will respond
 * @return this future
 */
public fun <T> CompletableFuture<T>.reactToExceptionIn(otherFuture: CompletableFuture<*>): CompletableFuture<T> {
  otherFuture.whenComplete { _, exception ->
    when (exception) {
      is CancellationException -> cancel(true)
      is Throwable -> completeExceptionally(exception)
    }
  }
  return this
}
