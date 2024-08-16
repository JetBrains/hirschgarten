package org.jetbrains.plugins.bsp.run

import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputType
import kotlinx.coroutines.Deferred
import java.io.OutputStream

class BspProcessHandler(private val runDeferred: Deferred<*>) : ProcessHandler() {
  override fun startNotify() {
    super.startNotify()
    runDeferred.invokeOnCompletion { e ->
      if (e == null) {
        notifyProcessTerminated(0)
      } else {
        notifyTextAvailable(e.toString(), ProcessOutputType.STDERR)
        notifyProcessTerminated(1)
      }
    }
  }

  override fun destroyProcessImpl() {
    runDeferred.cancel()
    notifyProcessTerminated(1)
  }

  override fun detachProcessImpl() {
    notifyProcessDetached()
  }

  override fun detachIsDefault(): Boolean = false

  override fun getProcessInput(): OutputStream? = null
}
