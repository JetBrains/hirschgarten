package org.jetbrains.bazel.languages.bazelquery.terminal

import com.intellij.terminal.completion.spec.ShellCommandParserOptions
import com.intellij.terminal.completion.spec.ShellCommandSpec
import com.intellij.terminal.completion.spec.ShellCompletionSuggestion
import com.intellij.terminal.completion.spec.ShellRuntimeContext
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.languages.bazelquery.completion.generateTargetCompletions
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenSets
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenType
import org.jetbrains.plugins.terminal.block.completion.spec.*

/*
TODO
- flags
- expressions
 */
internal fun bazelQueryCommandSpec(): ShellCommandSpec = ShellCommandSpec("bazel") {
  requiresSubcommand = true
  subcommands { context: ShellRuntimeContext ->

    // if(context.shellName.isBash())
    subcommand("query") {
      description("Executes a dependency graph query.")

      parserOptions = ShellCommandParserOptions.create(optionArgSeparators = listOf("=", " "))

      argument {
        displayName("query expression")
        val targetPriority = 40 // The greater, the closer to the first place, default is 50
        suggestions(ShellRuntimeDataGenerator { context: ShellRuntimeContext ->
          val offset = context.typedPrefix.lastIndexOfAny(charArrayOf('\'', '"', '(', ',', ' ')) + 1
          val typedPrefix = context.typedPrefix.substring(offset)
          val targets = generateTargetCompletions(typedPrefix, context.currentDirectory)
          val suggestions : MutableList<ShellCompletionSuggestion> = targets.map { target ->
            ShellCompletionSuggestion(
              name = target,
              icon = BazelPluginIcons.bazel,
              prefixReplacementIndex = offset,
              priority = targetPriority
            )
          }.toMutableList()
          suggestions.addAll(
            knownCommands.map {
              ShellCompletionSuggestion(
                name = "$it()",
                icon = BazelPluginIcons.bazel,
                prefixReplacementIndex = offset,
                insertValue = "$it({cursor})",
              )
            }
          )

          // Empty suggestion for the parser to consider quoted expression as valid argument, so flags will be suggested after argument.
          // Inspired from ShellDataGenerators#getFileSuggestions.
          if (isStartAndEndWithQuote(context.typedPrefix)) {
            val emptySuggestion = ShellCompletionSuggestion(name = "", prefixReplacementIndex = offset, isHidden = true)
            suggestions.add(emptySuggestion)
          }

          suggestions
        })
      }

      option("--aspect_deps") {
        separator = "="
        argument {
          displayName("option")
          suggestions("off", "conservative", "precise")
        }
      }
    }
  }
}

fun isStartAndEndWithQuote(expression: String) : Boolean {
  return expression.length >= 2 && ((expression.startsWith('\'') && expression.endsWith('\'')) || (expression.startsWith('"') && expression.endsWith('"')))
}

val knownCommands =
  BazelqueryTokenSets.COMMANDS.types.map { tokenType ->
    tokenType as BazelqueryTokenType
    tokenType.completionText()
  }.toList()

