package org.jetbrains.bazel.server.benchmark

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import io.opentelemetry.sdk.trace.data.SpanData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

// Utility functions taken from IDEA

inline fun <T> Span.useWithoutActiveScope(operation: (Span) -> T): T {
  try {
    return operation(this)
  } catch (e: CancellationException) {
    throw e
  } catch (e: Throwable) {
    setStatus(StatusCode.ERROR)
    throw e
  } finally {
    end()
  }
}

@Suppress("unused")
suspend inline fun <T> SpanBuilder.useWithScope(
  context: CoroutineContext = EmptyCoroutineContext,
  crossinline operation: suspend CoroutineScope.(Span) -> T,
): T =
  startSpan().useWithoutActiveScope { span ->
    // inner withContext to ensure that we report the end of the span only when all child tasks are completed
    withContext(context + Context.current().with(span).asContextElement()) {
      operation(span)
    }
  }

val SpanData.durationMs: Long
  get() = (this.endEpochNanos - this.startEpochNanos) / 1_000_000
