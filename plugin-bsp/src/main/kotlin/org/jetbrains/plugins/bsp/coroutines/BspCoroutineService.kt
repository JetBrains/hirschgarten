package org.jetbrains.plugins.bsp.coroutines

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
// @ApiStatus.Internal
public class BspCoroutineService(private val cs: CoroutineScope) {
  public fun start(callable: suspend () -> Unit): Job = cs.launch { callable() }

  public fun <T> startAsync(lazy: Boolean = false, callable: suspend () -> T): Deferred<T> =
    cs.async(start = if (lazy) CoroutineStart.LAZY else CoroutineStart.DEFAULT) { callable() }

  public companion object {
    @JvmStatic
    public fun getInstance(project: Project): BspCoroutineService = project.service<BspCoroutineService>()
  }
}
