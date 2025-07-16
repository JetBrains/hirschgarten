package org.jetbrains.bazel.languages.bazelquery.runAnything

import com.intellij.ide.actions.runAnything.RunAnythingAction
import com.intellij.ide.actions.runAnything.activity.RunAnythingCommandLineProvider
import com.intellij.ide.actions.runAnything.activity.RunAnythingCommandProvider
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.languages.bazelquery.completion.TargetCompletionsGenerator
import org.jetbrains.bazel.languages.bazelquery.completion.knownCommands
import org.jetbrains.bazel.languages.bazelquery.completion.knownOperations
import org.jetbrains.bazel.languages.bazelquery.options.BazelQueryCommonOptions
import javax.swing.Icon

class BazelQueryRunAnythingProvider : RunAnythingCommandLineProvider() {

  private enum class CompletionContext {
    EMPTY,
    INSIDE_EXPRESSION,
    AFTER_EXPRESSION,
    FLAG
  }

  override fun getHelpGroupTitle() = "Bazel"
  override fun getHelpCommandPlaceholder() = "bazel query <query-expression> <options>"
  override fun getHelpIcon(): Icon = BazelPluginIcons.bazelToolWindow
  override fun getHelpCommand() = "bazel query"
  override fun getIcon(value: String): Icon = BazelPluginIcons.bazelToolWindow
  override fun getCompletionGroupTitle() = "Bazel Query"
  override fun getMainListItem(dataContext: DataContext, value: String) =
    RunAnythingBazelQueryItem(getCommand(value), getIcon(value))

  override fun suggestCompletionVariants(dataContext: DataContext, commandLine: CommandLine): Sequence<String> {
    val project = dataContext.getData(CommonDataKeys.PROJECT) ?: return emptySequence()

    if (!commandLine.helpCommand.equals("bazel query", ignoreCase = true)) {
      return emptySequence()
    }

    val context = determineCompletionContext(commandLine)
    val toComplete = commandLine.toComplete

    return when (context) {
      CompletionContext.EMPTY ->
        functionSuggestions(toComplete) + targetSuggestions(project, toComplete) + flagSuggestions(toComplete)

      CompletionContext.INSIDE_EXPRESSION ->
        functionSuggestions(toComplete) + targetSuggestions(project, toComplete)

      CompletionContext.AFTER_EXPRESSION ->
        operatorSuggestions(toComplete) + flagSuggestions(toComplete)

      CompletionContext.FLAG ->
        flagSuggestions(toComplete)
    }
  }

  private fun determineCompletionContext(commandLine: CommandLine): CompletionContext {
    val command = commandLine.command.trim()
    val prefix = commandLine.prefix.trim()

    if (command.contains("--")) {
      return CompletionContext.FLAG
    }
    if (command.isEmpty()) {
      return CompletionContext.EMPTY
    }
    if (listOf("(", ",", "+", "^", "\\", "intersect", "union", "except", "equals", "in" ).none {
      prefix.endsWith(it)
    }) {
      return CompletionContext.AFTER_EXPRESSION
    }
    return CompletionContext.INSIDE_EXPRESSION
  }

  private fun functionSuggestions(prefix: String): Sequence<String> {
    return knownCommands.asSequence().filter { it.startsWith(prefix) }
  }

  private fun targetSuggestions(project: Project, prefix: String): Sequence<String> {
    return TargetCompletionsGenerator(project)
      .getTargetsList(prefix)
      .asSequence()
  }

  private fun flagSuggestions(prefix: String): Sequence<String> {
    return BazelQueryCommonOptions.getAll()
      .asSequence()
      .map { "--${it.name}" }
      .filter { it.startsWith(prefix) }
  }

  private fun operatorSuggestions(prefix: String): Sequence<String> {
    return knownOperations.asSequence().filter { it.startsWith(prefix) }
  }

  override fun run(dataContext: DataContext, commandLine: CommandLine): Boolean {
    val workDirectory = dataContext.getData(CommonDataKeys.VIRTUAL_FILE) ?: return false
    val executor = dataContext.getData(RunAnythingAction.EXECUTOR_KEY) ?: return false

    RunAnythingCommandProvider.runCommand(workDirectory, helpCommand + " " + commandLine.command, executor, dataContext)
    return true
  }
}
