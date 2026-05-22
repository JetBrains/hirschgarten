package org.jetbrains.bazel.coroutines

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.annotations.ApiStatus

/**
 * Application-level coroutine scope service for operations that must survive project closure.
 * Use this when you need to close a project and then open another one.
 */
@ApiStatus.Internal
@Service(Service.Level.APP)
class BazelApplicationCoroutineScopeService(private val coroutineScope: CoroutineScope) {
  fun launch(block: suspend CoroutineScope.() -> Unit): Job =
    coroutineScope.launch(
      start = CoroutineStart.UNDISPATCHED,
      block = block,
    )

  companion object {
    @JvmStatic
    fun getInstance(): BazelApplicationCoroutineScopeService = service<BazelApplicationCoroutineScopeService>()
  }
}
