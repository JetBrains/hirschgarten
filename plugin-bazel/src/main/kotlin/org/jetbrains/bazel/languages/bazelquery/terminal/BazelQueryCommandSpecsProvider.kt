package org.jetbrains.bazel.languages.bazelquery.terminal

import org.jetbrains.bazel.config.BazelFeatureFlags.isQueryTerminalCompletionEnabled
import org.jetbrains.plugins.terminal.block.completion.spec.ShellCommandSpecConflictStrategy
import org.jetbrains.plugins.terminal.block.completion.spec.ShellCommandSpecInfo
import org.jetbrains.plugins.terminal.block.completion.spec.ShellCommandSpecsProvider

@Suppress("UnstableApiUsage")
class BazelQueryCommandSpecsProvider : ShellCommandSpecsProvider {
  override fun getCommandSpecs(): List<ShellCommandSpecInfo> =
    if (isQueryTerminalCompletionEnabled) {
      listOf(ShellCommandSpecInfo.create(bazelQueryCommandSpec(), ShellCommandSpecConflictStrategy.OVERRIDE))
    } else {
      listOf()
    }
}
