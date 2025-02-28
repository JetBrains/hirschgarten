package org.jetbrains.bazel.install

import org.jetbrains.bazel.install.cli.CliOptions
import org.jetbrains.bazel.install.cli.CliOptionsProvider
import java.nio.file.Path

object Install {
  @JvmStatic
  fun main(args: Array<String>) {
    val cliOptionsProvider = CliOptionsProvider(args)
    val cliOptions = cliOptionsProvider.getOptions()

    if (cliOptions.helpCliOptions.isHelpOptionUsed) {
      cliOptions.helpCliOptions.printHelp()
    } else {
      runInstall(cliOptions)
    }
  }

  private fun runInstall(cliOptions: CliOptions) {
    InstallationContextProvider.generateAndSaveProjectViewFileIfNeeded(cliOptions)
    createEnvironment(cliOptions)
    printSuccess(cliOptions.workspaceRootDir)
  }

  private fun createEnvironment(cliOptions: CliOptions) {
    val environmentCreator = BazelBspEnvironmentCreator(cliOptions.workspaceRootDir)
    environmentCreator.create()
  }

  private fun printSuccess(workspaceRootDir: Path) {
    val absoluteDirWhereServerWasInstalledIn = workspaceRootDir.toAbsolutePath().normalize()
    println("Bazel BSP server installed in '$absoluteDirWhereServerWasInstalledIn'.")
  }
}
