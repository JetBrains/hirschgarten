package org.jetbrains.plugins.bsp.services

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.ui.configuration.BspConsolePrinter
import org.jetbrains.plugins.bsp.ui.configuration.test.BspTestConsole


public open class BspConsoleService<T : BspConsolePrinter> {

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

public class BspRunConsoleService : BspConsoleService<BspConsolePrinter>() {

  public companion object {
    public fun getInstance(project: Project): BspRunConsoleService =
      project.getService(BspRunConsoleService::class.java)
  }
}

public class BspTestConsoleService : BspConsoleService<BspTestConsole>() {

  public fun startTest(suite: Boolean, displayName: String) {
    consoleListeners.forEach{ it.startTest(suite, displayName) }
  }

  public fun failTest(displayName: String, message: String) {
    consoleListeners.forEach{ it.failTest(displayName, displayName) }
  }

  public fun passTest(suite: Boolean, displayName: String) {
    consoleListeners.forEach{ it.passTest(suite, displayName) }
  }

  public fun ignoreTest(displayName: String) {
    consoleListeners.forEach{ it.ignoreTest(displayName) }
  }

  public companion object {
    public fun getInstance(project: Project): BspTestConsoleService =
      project.getService(BspTestConsoleService::class.java)
  }
}