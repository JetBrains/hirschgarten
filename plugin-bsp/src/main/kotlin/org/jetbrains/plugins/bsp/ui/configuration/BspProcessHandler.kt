package org.jetbrains.plugins.bsp.ui.configuration

import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.execution.process.ProcessOutputType
import java.io.OutputStream

public interface BspConsolePrinter {
  public fun printOutput(text: String)
}

public class BspProcessHandler : ProcessHandler(), BspConsolePrinter {

  override fun destroyProcessImpl() {
    super.notifyProcessTerminated(0)
  }

  override fun detachProcessImpl() {
    notifyProcessDetached()
  }

  override fun detachIsDefault(): Boolean = false

  override fun getProcessInput(): OutputStream? = null

  override fun printOutput(text: String) {
    val output = prepareTextToPrint(text)
    notifyTextAvailable(output, ProcessOutputType.STDOUT)
  }

  private fun prepareTextToPrint(text: String): String =
    if (text.endsWith("\n")) text else text + "\n"

  public fun shutdown() {
    super.notifyProcessTerminated(0)
  }

  public fun execute(task: Runnable) {
    ProcessIOExecutorService.INSTANCE.submit(task)
  }
}
