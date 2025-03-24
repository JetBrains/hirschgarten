package org.jetbrains.bazel.languages.bazelquery.terminal

import com.intellij.markdown.utils.doc.DocMarkdownToHtmlConverter
import com.intellij.openapi.project.ProjectManager
import com.intellij.terminal.completion.spec.ShellCommandParserOptions
import com.intellij.terminal.completion.spec.ShellCommandSpec
import com.intellij.terminal.completion.spec.ShellCompletionSuggestion
import com.intellij.terminal.completion.spec.ShellRuntimeContext
import kotlinx.collections.immutable.toPersistentMap
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.bazelrunner.params.BazelFlag
import org.jetbrains.bazel.languages.bazelquery.completion.generateTargetCompletions
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenSets
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenType
import org.jetbrains.bazel.languages.bazelquery.functions.BazelqueryFunction
import org.jetbrains.bazel.languages.bazelquery.functions.BazelqueryFunctionSymbol
import org.jetbrains.bazel.languages.bazelquery.options.BazelqueryCommonOptions
import org.jetbrains.bazel.languages.bazelrc.documentation.BazelFlagDocumentationTarget
import org.jetbrains.bazel.languages.bazelrc.documentation.BazelFlagDocumentationTarget.Companion.allowMultiple
import org.jetbrains.bazel.languages.bazelrc.documentation.BazelFlagDocumentationTarget.Companion.default
import org.jetbrains.bazel.languages.bazelrc.documentation.BazelFlagDocumentationTarget.Companion.effects
import org.jetbrains.bazel.languages.bazelrc.documentation.BazelFlagDocumentationTarget.Companion.expandsTo
import org.jetbrains.bazel.languages.bazelrc.documentation.BazelFlagDocumentationTarget.Companion.help
import org.jetbrains.bazel.languages.bazelrc.documentation.BazelFlagDocumentationTarget.Companion.metadataTags
import org.jetbrains.bazel.languages.bazelrc.documentation.BazelFlagDocumentationTarget.Companion.oldName
import org.jetbrains.bazel.languages.bazelrc.documentation.BazelFlagDocumentationTarget.Companion.type
import org.jetbrains.bazel.languages.bazelrc.flags.BazelFlagSymbol
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.bazel.languages.bazelrc.flags.Flag.Companion.declaredFieldsMap
import org.jetbrains.bazel.languages.bazelrc.flags.KnownFlags
import org.jetbrains.bazel.languages.bazelrc.flags.Option
import org.jetbrains.bazel.languages.bazelrc.flags.knownFlagNames
import org.jetbrains.plugins.terminal.block.completion.spec.*

/*
TODO
- flags
- expressions
 */
@Suppress("UnstableApiUsage")
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
              // TODO not this copy paste from functions
              val markdownText =
                """
                  |${it.description}
                  |
                  |**Arguments:**
                  |${it.argumentsMarkdown()}
                  |
                  |**Example Usage:**
                  ```
                    ${if (it is BazelqueryFunction.SimpleFunction) it.exampleUsage else "N/A"}
                  ```
                """.trimMargin()
              val htmlText = DocMarkdownToHtmlConverter.convert(context.project, markdownText)

              ShellCompletionSuggestion(
                name = "${it.name}()",
                description = htmlText,
                icon = BazelPluginIcons.bazel,
                prefixReplacementIndex = offset,
                insertValue = "${it.name}({cursor})",
              )
            }
          )

          // Empty suggestion for the parser to consider quoted expression as valid argument, so flags will be suggested after the argument.
          // Inspired from ShellDataGenerators#getFileSuggestions.
          if (isStartAndEndWithQuote(context.typedPrefix)) {
            val emptySuggestion = ShellCompletionSuggestion(name = "", prefixReplacementIndex = offset, isHidden = true)
            suggestions.add(emptySuggestion)
          }

          suggestions
        })
      }


      knownOptions.forEach {
        val flagName = "--${it.name}"
        val flag = Flag.byName(flagName)!!
        // TODO not this copy paste from BazelFlagDocumentationTarget
        val markdownText =
          """
            |
            |${flag.type()}
            |
            |${flag.default()}
            |
            |${flag.oldName()}
            |
            |${flag.allowMultiple()}
            |
            |${flag.help()}
            |
            |${flag.expandsTo()}
            |
            |${flag.effects()}
            |
            |${flag.metadataTags()}
            """.trimMargin()
        val htmlText = DocMarkdownToHtmlConverter.convert(context.project, markdownText)
        option(flagName) {
          // TODO maybe add arguments for some flags
          description(htmlText)
        }
      }
    }
  }
}

fun isStartAndEndWithQuote(expression: String) : Boolean {
  return expression.length >= 2 && ((expression.startsWith('\'') && expression.endsWith('\'')) || (expression.startsWith('"') && expression.endsWith('"')))
}

val knownCommands = BazelqueryFunction.getAll()

private fun BazelqueryFunction.argumentsMarkdown(): String =
  arguments.joinToString(separator = "\n") { arg ->
    "- `${arg.name}` (${arg.type}${if (arg.optional) ", optional" else ""}): ${arg.description}"
  }

val knownOptions = BazelqueryCommonOptions().getAll()
