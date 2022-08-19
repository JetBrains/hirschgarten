package org.jetbrains.plugins.bsp.ui.console
// Temporary location - another package might be more suitable

import java.io.OutputStream

public class ConsoleOutputStream(private val id: Any?, private val bspSyncConsole: BspSyncConsole) : OutputStream() {
  private var line: StringBuffer = StringBuffer()

  override fun write(b: Int) {
    val c: Char = (b and 255).toChar()
    line.append(c)
    if (c == '\n') {
      bspSyncConsole.addMessage(id, line.toString())
      line = StringBuffer()
    }
  }
}
