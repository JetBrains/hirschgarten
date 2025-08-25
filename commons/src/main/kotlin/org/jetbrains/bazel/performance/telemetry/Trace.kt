package org.jetbrains.bazel.performance.telemetry

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Starts a new span and adds it to the current scope for the [operation].
 * That way the spans created inside the [operation] will be nested to the created span.
 *
 * See [span concept](https://opentelemetry.io/docs/concepts/signals/traces/#spans) for more details on span nesting.
 */
inline fun <T> SpanBuilder.use(operation: (Span) -> T): T =
  startSpan().useWithoutActiveScope { span ->
    span.makeCurrent().use {
      operation(span)
    }
  }

/**
 * Starts a new span and adds it to the current scope.
 * That way the spans created inside the [operation] will be nested to the created span.
 *
 * See [span concept](https://opentelemetry.io/docs/concepts/signals/traces/#spans) for more details on span nesting.
 */
inline fun <T> Span.use(operation: (Span) -> T): T =
  useWithoutActiveScope {
    makeCurrent().use {
      operation(this)
    }
  }

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

/**
 * Do not use it.
 * Only for implementation.
 *
 * Does not activate the span scope, so **new spans created inside [operation] will not be linked to [this] span**.
 * [Span] supplied as an argument to [operation] may not be the [Span.current].
 * No overhead of [Context.makeCurrent] is incurred.
 *
 * Consider using [use] to also activate the scope.
 */
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
