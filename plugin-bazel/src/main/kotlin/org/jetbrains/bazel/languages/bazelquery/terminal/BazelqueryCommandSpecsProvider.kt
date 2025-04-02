package org.jetbrains.bazel.languages.bazelquery.terminal

import com.intellij.terminal.completion.spec.*
import org.jetbrains.plugins.terminal.block.completion.spec.ShellCommandSpecConflictStrategy
import org.jetbrains.plugins.terminal.block.completion.spec.ShellCommandSpecInfo
import org.jetbrains.plugins.terminal.block.completion.spec.ShellCommandSpecsProvider

@Suppress("UnstableApiUsage")
class BazelqueryCommandSpecsProvider : ShellCommandSpecsProvider {
  override fun getCommandSpecs(): List<ShellCommandSpecInfo> = listOf(
    ShellCommandSpecInfo.create(bazelQueryCommandSpec(), ShellCommandSpecConflictStrategy.OVERRIDE)
  )
}
