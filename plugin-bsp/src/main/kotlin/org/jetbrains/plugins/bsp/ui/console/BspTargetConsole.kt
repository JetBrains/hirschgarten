package org.jetbrains.plugins.bsp.ui.console

import org.jetbrains.plugins.bsp.run.BspConsolePrinter
import org.jetbrains.plugins.bsp.run.BspTestConsolePrinter

public open class BspTargetConsole<T : BspConsolePrinter> {
  protected val consoleListeners: MutableSet<T> = mutableSetOf()

  public fun registerPrinter(printer: T) {
    consoleListeners.add(printer)
  }

  public fun deregisterPrinter(printer: T) {
    consoleListeners.remove(printer)
  }

  public fun print(text: String) {
    consoleListeners.forEach { it.printOutput(text) }
  }
}

public class BspTargetRunConsole : BspTargetConsole<BspConsolePrinter>()

public class BspTargetTestConsole : BspTargetConsole<BspTestConsolePrinter>() {
  public fun startTest(suite: Boolean, displayName: String) {
    consoleListeners.forEach { it.startTest(suite, displayName) }
  }

  public fun failTest(displayName: String, message: String) {
    consoleListeners.forEach { it.failTest(displayName, message) }
  }

  public fun passTest(suite: Boolean, displayName: String) {
    consoleListeners.forEach { it.passTest(suite, displayName) }
  }

  public fun ignoreTest(displayName: String) {
    consoleListeners.forEach { it.ignoreTest(displayName) }
  }
}
