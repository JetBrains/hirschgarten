package org.jetbrains.plugins.bsp.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.annotations.ApiStatus

@Service(Service.Level.PROJECT)
@ApiStatus.Internal
internal class BspCoroutineService(private val cs: CoroutineScope) {
  fun start(callable: suspend () -> Unit) {
    cs.launch { callable() }
  }

  companion object {
    @JvmStatic
    fun getInstance() = service<BspCoroutineService>()
  }
}
