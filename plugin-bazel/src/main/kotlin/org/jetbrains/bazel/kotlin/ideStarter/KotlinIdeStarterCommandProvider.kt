package org.jetbrains.bazel.kotlin.ideStarter

import com.jetbrains.performancePlugin.CommandProvider
import com.jetbrains.performancePlugin.CreateCommand

class KotlinIdeStarterCommandProvider : CommandProvider {
  override fun getCommands(): Map<String, CreateCommand> =
    mapOf(
      EnableKotlinCoroutineDebugCommand.PREFIX to CreateCommand(::EnableKotlinCoroutineDebugCommand),
      CreateDirectoryCommand.PREFIX to CreateCommand(::CreateDirectoryCommand),
      MoveClassCommand.PREFIX to CreateCommand(::MoveClassCommand),
    )
}
