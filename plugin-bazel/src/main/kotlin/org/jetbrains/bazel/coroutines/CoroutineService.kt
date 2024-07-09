package org.jetbrains.bazel.coroutines

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jetbrains.annotations.ApiStatus

@Service(Service.Level.PROJECT)
@ApiStatus.Internal
internal class CoroutineService(private val cs: CoroutineScope) {
  fun start(callable: suspend () -> Unit): Job = cs.launch { callable() }
  fun <T> startAsync(callable: suspend () -> T): Deferred<T> = cs.async { callable() }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): CoroutineService = project.service()
  }
}