package org.jetbrains.bazel.coroutines

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jetbrains.annotations.ApiStatus

@Service(Service.Level.PROJECT)
@ApiStatus.Internal
class BazelCoroutineService(private val coroutineScope: CoroutineScope) {
  fun start(block: suspend CoroutineScope.() -> Unit): Job =
    coroutineScope.launch(context = currentTelemetryContext(), block = block)

  fun <T> startAsync(lazy: Boolean = false, callable: suspend CoroutineScope.() -> T): Deferred<T> =
    coroutineScope.async(
      context = currentTelemetryContext(),
      start = if (lazy) CoroutineStart.LAZY else CoroutineStart.DEFAULT,
      block = callable,
    )

  private fun currentTelemetryContext() = Context.current().asContextElement()

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BazelCoroutineService = project.service<BazelCoroutineService>()
  }
}
