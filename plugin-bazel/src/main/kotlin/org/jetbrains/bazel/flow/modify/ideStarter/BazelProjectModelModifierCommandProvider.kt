package org.jetbrains.bazel.flow.modify.ideStarter

import com.jetbrains.performancePlugin.CommandProvider
import com.jetbrains.performancePlugin.CreateCommand

class BazelProjectModelModifierCommandProvider : CommandProvider {
  override fun getCommands(): Map<String, CreateCommand?> =
    mapOf(
      ApplyOrderEntryQuickFixCommand.PREFIX to CreateCommand(::ApplyOrderEntryQuickFixCommand),
    )
}
