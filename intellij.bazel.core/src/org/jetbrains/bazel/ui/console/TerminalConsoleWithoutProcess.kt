package org.jetbrains.bazel.ui.console

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.terminal.TerminalExecutionConsole
import com.jediterm.core.util.TermSize
import com.jediterm.terminal.TtyConnector

/**
 * TerminalExecutionConsoleBuilder expects a `BaseProcessHandler`.
 * `BazelProcessHandler` doesn't extend from it, because can first call `bazel info` and other things before actually calling `bazel run`.
 * So we provide a stub here. Also see https://github.com/bazelbuild/intellij/pull/7001/files
 */
internal fun TerminalExecutionConsole.startSessionWithoutProcess() {
  val ttyConnector = object : TtyConnector, Disposable {
    private var connected: Boolean = true

    override fun dispose() {
      connected = false
    }

    override fun read(buf: CharArray?, offset: Int, length: Int): Int = 0
    override fun write(bytes: ByteArray?) = Unit
    override fun write(string: String?) = Unit
    override fun isConnected(): Boolean = connected
    override fun waitFor(): Int = 0
    override fun ready(): Boolean = true
    override fun getName(): String = ""
    override fun close() = Unit
    override fun resize(termSize: TermSize) = Unit
  }
  Disposer.register(this, ttyConnector)

  terminalWidget.createTerminalSession(ttyConnector)
  terminalWidget.start()
}
