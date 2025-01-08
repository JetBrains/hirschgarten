package org.jetbrains.bsp.bazel.server.benchmark

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.semconv.ExceptionAttributes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

// Utility functions taken from IDEA

@Suppress("unused")
suspend inline fun <T> SpanBuilder.useWithScope(
  context: CoroutineContext = EmptyCoroutineContext,
  crossinline operation: suspend CoroutineScope.(Span) -> T,
): T {
  val span = startSpan()
  return withContext(Context.current().with(span).asContextElement() + context) {
    try {
      operation(span)
    } catch (e: CancellationException) {
      span.recordException(e, Attributes.of(ExceptionAttributes.EXCEPTION_ESCAPED, true))
      throw e
    } catch (e: Throwable) {
      span.recordException(e, Attributes.of(ExceptionAttributes.EXCEPTION_ESCAPED, true))
      span.setStatus(StatusCode.ERROR)
      throw e
    } finally {
      span.end()
    }
  }
}

val SpanData.durationMs: Long
  get() = (this.endEpochNanos - this.startEpochNanos) / 1_000_000
