package org.jetbrains.bazel.performanceImpl

import com.jetbrains.performancePlugin.CommandProvider
import com.jetbrains.performancePlugin.CreateCommand

public class BazelCommandProvider : CommandProvider {
  override fun getCommands(): Map<String, CreateCommand> =
    mapOf(
      WaitForBazelSyncCommand.PREFIX to CreateCommand(::WaitForBazelSyncCommand),
      BuildAndSyncCommand.PREFIX to CreateCommand(::BuildAndSyncCommand),
      StartRecordingMaxMemoryCommand.PREFIX to CreateCommand(::StartRecordingMaxMemoryCommand),
      StopRecordingMaxMemoryCommand.PREFIX to CreateCommand(::StopRecordingMaxMemoryCommand),
      RecordMemoryCommand.PREFIX to CreateCommand(::RecordMemoryCommand),
      OpenBspToolWindowCommand.PREFIX to CreateCommand(::OpenBspToolWindowCommand),
      AssertFileContentsEqualCommand.PREFIX to CreateCommand(::AssertFileContentsEqualCommand),
      AssertEitherFileContentIsEqualCommand.PREFIX to CreateCommand(::AssertEitherFileContentIsEqualCommand)
    )
}
