package org.jetbrains.bazel.install

import org.jetbrains.bazel.commons.constants.Constants.DEFAULT_PROJECT_VIEW_FILE_NAME
import org.jetbrains.bazel.install.cli.CliOptions
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

object InstallationContextProvider {
  fun generateAndSaveProjectViewFileIfNeeded(cliOptions: CliOptions) {
    val generatedProjectViewFilePath = calculateGeneratedProjectViewPath(cliOptions)
    if (!generatedProjectViewFilePath.isFileExisted() || cliOptions.projectViewCliOptions != null) {
      if (!generatedProjectViewFilePath.exists()) {
        ProjectViewCLiOptionsProvider.generateProjectViewAndSave(cliOptions, generatedProjectViewFilePath)
      }
    }
  }

  private fun calculateGeneratedProjectViewPath(cliOptions: CliOptions): Path =
    cliOptions.projectViewFilePath ?: cliOptions.workspaceRootDir.resolve(DEFAULT_PROJECT_VIEW_FILE_NAME)

  private fun Path.isFileExisted() = this.exists() && this.isRegularFile()
}
