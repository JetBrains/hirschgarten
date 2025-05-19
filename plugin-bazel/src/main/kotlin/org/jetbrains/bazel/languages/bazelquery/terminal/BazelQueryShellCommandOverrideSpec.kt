package org.jetbrains.bazel.languages.bazelquery.terminal

import com.intellij.markdown.utils.doc.DocMarkdownToHtmlConverter
import com.intellij.openapi.project.Project
import com.intellij.terminal.completion.spec.ShellCommandParserOptions
import com.intellij.terminal.completion.spec.ShellCommandSpec
import com.intellij.terminal.completion.spec.ShellCompletionSuggestion
import com.intellij.terminal.completion.spec.ShellRuntimeContext
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.languages.bazelquery.completion.TargetCompletionsGenerator
import org.jetbrains.bazel.languages.bazelquery.documentation.BazelQueryFunctionDocumentationTarget
import org.jetbrains.bazel.languages.bazelquery.functions.BazelQueryFunction
import org.jetbrains.bazel.languages.bazelquery.options.BazelQueryCommonOptions
import org.jetbrains.bazel.languages.bazelrc.documentation.BazelFlagDocumentationTarget
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.plugins.terminal.block.completion.spec.ShellCommandSpec
import org.jetbrains.plugins.terminal.block.completion.spec.ShellCompletionSuggestion
import org.jetbrains.plugins.terminal.block.completion.spec.ShellRuntimeDataGenerator
import org.jetbrains.plugins.terminal.block.completion.spec.dsl.ShellCommandContext
import org.jetbrains.plugins.terminal.block.completion.spec.project
import java.nio.file.Path

/*
 *  All tokens must be treated as known by parser. The terminal stops suggesting if a token is unknown.
 *  Make token known by providing suggestion for it (sometimes we must provide empty suggestion like in ShellDataGenerators#getFileSuggestions).
 *  TODO Some options-args should be separated with space or =, we cannot do with both 2 for now, =sign alone not working for now
*/
@Suppress("UnstableApiUsage")
internal fun bazelQueryCommandSpec(): ShellCommandSpec =
  ShellCommandSpec("bazel") {
    subcommands { context: ShellRuntimeContext ->

      subcommand("query") {
        parserOptions = ShellCommandParserOptions.create(optionArgSeparators = listOf("=", " "))
        description("Executes a dependency graph query.")

        allOptions(context)

        // This dummyArgs surrounding is to make terminal still suggests even if we typed 'unknown tokens', e.g. options arguments like integer or comma-seperated
        dummyArgs()

        queryCompletion()

        dummyArgs()
      }
    }
  }

@Suppress("UnstableApiUsage")
private fun ShellCommandContext.dummyArgs() {
  argument {
    displayName("option")
    isVariadic = true
    isOptional = true
    suggestions(
      ShellRuntimeDataGenerator { context ->
        listOf(ShellCompletionSuggestion(name = context.typedPrefix, isHidden = true))
      },
    )
  }
}

@Suppress("UnstableApiUsage")
private fun ShellCommandContext.queryCompletion() {
  argument {
    displayName("query expression")
    val targetPriority = 40 // The greater, the closer to the first place, default is 50
    suggestions(
      ShellRuntimeDataGenerator { context: ShellRuntimeContext ->
        val offset = context.typedPrefix.lastIndexOfAny(charArrayOf('\'', '"', '(', ',', ' ', '^', '+', '-')) + 1
        val typedPrefix = context.typedPrefix.substring(offset)
        val targets =
          TargetCompletionsGenerator(context.project)
            .getTargetsList(typedPrefix, Path.of(context.currentDirectory))
        val suggestions: MutableList<ShellCompletionSuggestion> =
          targets
            .map { target ->
              ShellCompletionSuggestion(
                name = target,
                icon = BazelPluginIcons.bazel,
                prefixReplacementIndex = offset,
                priority = targetPriority,
              )
            }.toMutableList()
        suggestions.addAll(
          knownCommands.map {
            ShellCompletionSuggestion(
              name = "${it.name}()",
              description = functionDescriptionHtml(it, context.project),
              icon = BazelPluginIcons.bazel,
              prefixReplacementIndex = offset,
              insertValue = "${it.name}({cursor})",
            )
          },
        )

        // Empty suggestion for the parser to consider quoted expression as valid argument, so flags will be suggested after the argument.
        // Inspired from ShellDataGenerators#getFileSuggestions.
        if (isStartAndEndWithQuote(context.typedPrefix)) {
          val emptySuggestion = ShellCompletionSuggestion(name = "", prefixReplacementIndex = offset, isHidden = true)
          suggestions.add(emptySuggestion)
        }

        suggestions
      },
    )
  }
}

@Suppress("UnstableApiUsage")
private fun ShellCommandContext.allOptions(context: ShellRuntimeContext) {
  knownOptions.forEach { queryFlag ->
    val flagNameDD = "--${queryFlag.name}"
    val flag = Flag.byName(flagNameDD)
    if (flag != null) {
      when (flag) {
        // Should be done with `separator = "="` and arguments, but currently not working IJPL-150188
        // Cannot hide options
        is Flag.Boolean -> {
          booleanAndTriStateFlagSuggestion(flag, context)
        }

        is Flag.TriState -> {
          booleanAndTriStateFlagSuggestion(flag, context)
        }

        is Flag.OneOf -> {
          option(flagNameDD) {
            description(flagDescriptionHtml(flag, context.project))
            argument {
              suggestions {
                queryFlag.values.map {
                  ShellCompletionSuggestion(it)
                }
              }
            }
          }
        }

        is Flag.Unknown -> {
          option(flagNameDD) {
            description(flagDescriptionHtml(flag, context.project))
          }
        }

        else -> {
          optionWithUnknownArgs(flag, context.project)
        }
      }
    }
  }
}

@Suppress("UnstableApiUsage")
private fun ShellCommandContext.optionWithUnknownArgs(flag: Flag, project: Project) {
  option("--${flag.option.name}") {
    description(flagDescriptionHtml(flag, project))
    argument {
      isOptional = true
      suggestions {
        listOf(ShellCompletionSuggestion(name = "", isHidden = true))
      }
    }
  }
}

@Suppress("UnstableApiUsage")
private fun ShellCommandContext.booleanAndTriStateFlagSuggestion(flag: Flag, context: ShellRuntimeContext) {
  val trueFlag = "--${flag.option.name}"
  val falseFlag = "--no${flag.option.name}"

  option(trueFlag) {
    description(flagDescriptionHtml(flag, context.project))
    argument {
      if (flag is Flag.Boolean) {
        isOptional = true
      }
      suggestions("true", "yes", "1", "false", "no", "0")
    }
    exclusiveOn = listOf(falseFlag)
  }

  option(falseFlag) {
    description(flagDescriptionHtml(flag, context.project))
    exclusiveOn = listOf(trueFlag)
  }
}

private fun flagDescriptionHtml(flag: Flag, project: Project): String {
  val markdownText = BazelFlagDocumentationTarget.flagToDocumentationMarkdownText(flag)
  return DocMarkdownToHtmlConverter.convert(project, markdownText)
}

private fun functionDescriptionHtml(function: BazelQueryFunction, project: Project): String {
  val markdownText = BazelQueryFunctionDocumentationTarget.functionToDocumentationMarkdownText(function)
  return DocMarkdownToHtmlConverter.convert(project, markdownText)
}

private fun isStartAndEndWithQuote(expression: String): Boolean =
  expression.length >= 2 &&
    ((expression.startsWith('\'') && expression.endsWith('\'')) || (expression.startsWith('"') && expression.endsWith('"')))

private val knownCommands = BazelQueryFunction.getAll()

private fun BazelQueryFunction.argumentsMarkdown(): String =
  arguments.joinToString(separator = "\n") { arg ->
    "- `${arg.name}` (${arg.type}${if (arg.optional) ", optional" else ""}): ${arg.description}"
  }

private val knownOptions = BazelQueryCommonOptions.getAll()
