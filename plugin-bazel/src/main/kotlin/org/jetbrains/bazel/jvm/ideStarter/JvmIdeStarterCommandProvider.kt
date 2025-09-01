package org.jetbrains.bazel.jvm.ideStarter

import com.jetbrains.performancePlugin.CommandProvider
import com.jetbrains.performancePlugin.CreateCommand

class JvmIdeStarterCommandProvider : CommandProvider {
  override fun getCommands(): Map<String, CreateCommand> =
    mapOf(
      EnableHotswapCommand.PREFIX to CreateCommand(::EnableHotswapCommand),
    )
}
