package org.jetbrains.bazel.hotswap.ideStarter

import com.jetbrains.performancePlugin.CommandProvider
import com.jetbrains.performancePlugin.CreateCommand

internal class HotSwapCommandProvider : CommandProvider {
  override fun getCommands(): Map<String, CreateCommand> =
    mapOf(
      DebugLocalJvmSimpleKotlinTestCommand.PREFIX to CreateCommand(::DebugLocalJvmSimpleKotlinTestCommand),
    )
}
