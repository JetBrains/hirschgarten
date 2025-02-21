package org.jetbrains.bazel.workspace.ideStarter

import com.jetbrains.performancePlugin.CommandProvider
import com.jetbrains.performancePlugin.CreateCommand

class WorkspaceCommandProvider : CommandProvider {
  override fun getCommands(): Map<String, CreateCommand?> =
    mapOf(
      CheckOpenedFileNotInsideJarCommand.PREFIX to CreateCommand(::CheckOpenedFileNotInsideJarCommand),
    )
}
