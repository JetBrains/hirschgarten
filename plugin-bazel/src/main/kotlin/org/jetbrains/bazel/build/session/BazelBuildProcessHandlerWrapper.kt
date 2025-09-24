package org.jetbrains.bazel.build.session

import com.intellij.build.process.BuildProcessHandler
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Key
import org.jetbrains.annotations.NotNull
import java.io.OutputStream

/**
 * Adapts a generic ProcessHandler (e.g., OSProcessHandler) to a BuildProcessHandler
 * so it can be attached to a DefaultBuildDescriptor and controlled from the Build View.
 *
 * Important: we forward process output through this wrapper (with ANSI decoding) to avoid
 * duplicated console lines and mixed ANSI handling.
 */
internal class BazelBuildProcessHandlerWrapper(
  private val delegate: ProcessHandler,
) : BuildProcessHandler() {

  private val ansiDecoder = AnsiEscapeDecoder()

  init {
    // Bridge delegate events to this wrapper so Build View listeners attached to the wrapper
    // receive decoded text and lifecycle notifications exactly once.
    delegate.addProcessListener(object : ProcessListener {
      override fun startNotified(event: ProcessEvent) {
        // no-op: wrapper listeners will be attached by Build View after start
      }

      override fun processTerminated(event: ProcessEvent) {
        notifyProcessTerminated(event.exitCode)
      }

      override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {
        // no-op
      }

      override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        ansiDecoder.escapeText(event.text, outputType) { decodedText, attributes ->
          // Re-dispatch decoded text through the wrapper to all attached listeners
          notifyTextAvailable(decodedText, attributes)
        }
      }
    })
  }

  override fun destroyProcess() {
    delegate.destroyProcess()
  }

  override fun detachProcess() {
    delegate.detachProcess()
  }

  override fun isProcessTerminated(): Boolean = delegate.isProcessTerminated

  override fun isProcessTerminating(): Boolean = delegate.isProcessTerminating

  override fun getExitCode(): Int? = delegate.exitCode

  override fun getExecutionName(): String = "Bazel build"

  override fun destroyProcessImpl() {
    delegate.destroyProcess()
  }

  override fun detachProcessImpl() {
    delegate.detachProcess()
  }

  override fun detachIsDefault(): Boolean = delegate.detachIsDefault()

  override fun getProcessInput(): OutputStream? = delegate.processInput

  // Attach listeners to this wrapper (not to the delegate) so output is funneled via the bridging listener above
  override fun addProcessListener(listener: @NotNull ProcessListener) {
    super.addProcessListener(listener)
  }

  override fun addProcessListener(listener: @NotNull ProcessListener, parentDisposable: @NotNull Disposable) {
    super.addProcessListener(listener, parentDisposable)
  }
}
