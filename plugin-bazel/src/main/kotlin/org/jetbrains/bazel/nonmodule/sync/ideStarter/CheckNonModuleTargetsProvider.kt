package org.jetbrains.bazel.nonmodule.sync.ideStarter

import com.jetbrains.performancePlugin.CommandProvider
import com.jetbrains.performancePlugin.CreateCommand

class CheckNonModuleTargetsProvider : CommandProvider {
  override fun getCommands(): Map<String, CreateCommand?> =
    mapOf(
      CheckNonModuleTargetsCommand.PREFIX to CreateCommand(::CheckNonModuleTargetsCommand),
    )
}
