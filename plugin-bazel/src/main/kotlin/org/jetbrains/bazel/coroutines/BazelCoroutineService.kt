package org.jetbrains.bazel.coroutines

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jetbrains.bazel.annotations.InternalApi

@Service(Service.Level.PROJECT)
@InternalApi
class BazelCoroutineService(val coroutineScope: CoroutineScope) {
  fun start(block: suspend CoroutineScope.() -> Unit): Job = coroutineScope.launch(block = block)

  fun <T> startAsync(lazy: Boolean = false, callable: suspend () -> T): Deferred<T> =
    coroutineScope.async(start = if (lazy) CoroutineStart.LAZY else CoroutineStart.DEFAULT) { callable() }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BazelCoroutineService = project.service<BazelCoroutineService>()
  }
}
