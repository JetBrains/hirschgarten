package org.jetbrains.bazel.install

import org.jetbrains.bazel.commons.constants.Constants.DEFAULT_PROJECT_VIEW_FILE_NAME
import org.jetbrains.bazel.install.cli.CliOptions
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

object InstallationContextProvider {
  fun calculateProjectViewPath(workspaceDir: Path, projectViewFilePath: Path?): Path =
    projectViewFilePath ?: workspaceDir.resolve(DEFAULT_PROJECT_VIEW_FILE_NAME)

  private fun Path.isFileExisted() = this.exists() && this.isRegularFile()
}
