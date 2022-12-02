package org.jetbrains.plugins.bsp.runConfig

import com.intellij.execution.process.*
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.util.Key
import java.io.OutputStream

public class BspProcessHandler(private val console: ConsoleView) : ProcessHandler() {

  init {
    addProcessListener(object : ProcessAdapter() {
      override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        console.print(event.text, ConsoleViewContentType.getConsoleViewType(outputType))
      }
    })
  }

  override fun destroyProcessImpl() {
    super.notifyProcessTerminated(0)
  }

  override fun detachProcessImpl() {
    notifyProcessDetached()
  }

  override fun detachIsDefault(): Boolean = false

  override fun getProcessInput(): OutputStream? = null

  public fun shutdown() {
    super.notifyProcessTerminated(0)
  }
}
