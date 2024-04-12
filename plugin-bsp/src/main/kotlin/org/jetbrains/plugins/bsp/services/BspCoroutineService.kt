package org.jetbrains.plugins.bsp.services

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
public class BspCoroutineService(private val cs: CoroutineScope) {
  public fun start(callable: suspend () -> Unit): Job = cs.launch { callable() }
  public fun <T> startAsync(callable: suspend () -> T): Deferred<T> = cs.async { callable() }

  public companion object {
    @JvmStatic
    public fun getInstance(project: Project): BspCoroutineService = project.service<BspCoroutineService>()
  }
}
