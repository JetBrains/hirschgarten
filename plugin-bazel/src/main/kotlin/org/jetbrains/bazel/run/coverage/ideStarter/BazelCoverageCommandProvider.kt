package org.jetbrains.bazel.run.coverage.ideStarter

import com.jetbrains.performancePlugin.CommandProvider
import com.jetbrains.performancePlugin.CreateCommand

class BazelCoverageCommandProvider : CommandProvider {
  override fun getCommands(): Map<String, CreateCommand?> =
    mapOf(
      RunTestWithCoverageCommand.PREFIX to CreateCommand(::RunTestWithCoverageCommand),
      AssertCoverageCommand.PREFIX to CreateCommand(::AssertCoverageCommand),
    )
}
