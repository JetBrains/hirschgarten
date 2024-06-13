package org.jetbrains.plugins.bsp.performance.testing

import com.jetbrains.performancePlugin.CommandProvider
import com.jetbrains.performancePlugin.CreateCommand

public class BazelCommandProvider : CommandProvider {
  override fun getCommands(): Map<String, CreateCommand> = mapOf(
    WaitForBazelSyncCommand.PREFIX to CreateCommand(::WaitForBazelSyncCommand),
    StartMemoryProfilingCommand.PREFIX to CreateCommand(::StartMemoryProfilingCommand),
    StopMemoryProfilingCommand.PREFIX to CreateCommand(::StopMemoryProfilingCommand),
    OpenBspToolWindowCommand.PREFIX to CreateCommand(::OpenBspToolWindowCommand),
  )
}
