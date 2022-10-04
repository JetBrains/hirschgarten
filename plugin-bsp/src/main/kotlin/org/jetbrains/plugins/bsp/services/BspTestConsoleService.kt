package org.jetbrains.plugins.bsp.services

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.ui.test.configuration.BspTestConsole

public class BspTestConsoleService(project: Project) {

  public val bspTestConsole: BspTestConsole = BspTestConsole()

  public companion object {
    public fun getInstance(project: Project): BspTestConsoleService =
      project.getService(BspTestConsoleService::class.java)
  }
}
