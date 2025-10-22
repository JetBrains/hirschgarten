package org.jetbrains.bazel.install

import org.jetbrains.bazel.install.cli.CliOptions
import java.nio.file.Path

object Install {
  fun runInstall(cliOptions: CliOptions, silent: Boolean = false) {
    createEnvironment(cliOptions)
    if (!silent) printSuccess(cliOptions.workspaceDir)
  }

  private fun createEnvironment(cliOptions: CliOptions) {
    val environmentCreator = EnvironmentCreator(cliOptions.workspaceDir)
    environmentCreator.create()
  }

  private fun printSuccess(workspaceRootDir: Path) {
    val absoluteDirWhereServerWasInstalledIn = workspaceRootDir.toAbsolutePath().normalize()
    println("Bazel BSP server installed in '$absoluteDirWhereServerWasInstalledIn'.")
  }
}
