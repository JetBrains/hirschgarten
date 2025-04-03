package org.jetbrains.bazel.ui.ideStarter

import com.jetbrains.performancePlugin.CommandProvider
import com.jetbrains.performancePlugin.CreateCommand

class UITestCommandProvider : CommandProvider {
  override fun getCommands(): Map<String, CreateCommand> =
    mapOf(
      RunSimpleKotlinTestCommand.PREFIX to CreateCommand(::RunSimpleKotlinTestCommand),
    )
}
