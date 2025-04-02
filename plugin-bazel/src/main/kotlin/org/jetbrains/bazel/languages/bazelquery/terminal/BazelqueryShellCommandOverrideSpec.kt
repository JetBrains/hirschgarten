package org.jetbrains.bazel.languages.bazelquery.terminal

import com.intellij.markdown.utils.doc.DocMarkdownToHtmlConverter
import com.intellij.openapi.project.Project
import com.intellij.terminal.completion.spec.ShellCommandParserOptions
import com.intellij.terminal.completion.spec.ShellCommandSpec
import com.intellij.terminal.completion.spec.ShellCompletionSuggestion
import com.intellij.terminal.completion.spec.ShellRuntimeContext
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.languages.bazelquery.completion.generateTargetCompletions
import org.jetbrains.bazel.languages.bazelquery.functions.BazelqueryFunction
import org.jetbrains.bazel.languages.bazelquery.options.BazelqueryCommonOptions
import org.jetbrains.bazel.languages.bazelrc.documentation.BazelFlagDocumentationTarget.Companion.allowMultiple
import org.jetbrains.bazel.languages.bazelrc.documentation.BazelFlagDocumentationTarget.Companion.default
import org.jetbrains.bazel.languages.bazelrc.documentation.BazelFlagDocumentationTarget.Companion.effects
import org.jetbrains.bazel.languages.bazelrc.documentation.BazelFlagDocumentationTarget.Companion.expandsTo
import org.jetbrains.bazel.languages.bazelrc.documentation.BazelFlagDocumentationTarget.Companion.help
import org.jetbrains.bazel.languages.bazelrc.documentation.BazelFlagDocumentationTarget.Companion.metadataTags
import org.jetbrains.bazel.languages.bazelrc.documentation.BazelFlagDocumentationTarget.Companion.oldName
import org.jetbrains.bazel.languages.bazelrc.documentation.BazelFlagDocumentationTarget.Companion.type
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.plugins.terminal.block.completion.spec.*
import org.jetbrains.plugins.terminal.block.completion.spec.dsl.ShellCommandContext

/*
TODO
- flags
- expressions
 */

/*
  *  All tokens must be treated as known by parser. The terminal stops suggesting if a token is unknown.
  *  Make token known by providing suggestion for it (sometimes we must provide empty suggestion like in ShellDataGenerators#getFileSuggestions).
  *  In arguments we can have context (typed prefix, project, shell name).
  *  In options we don't have them, so we can only provide static suggestions.
  *  We can treat options as arguments, so we can provide suggestions based on context, (add TerminalIcons.Option to make it appear as options),
  but we would not be able to exclude used options (for now, an issue was submitted).
  *  TODO Some options-args should be separated with space or =, we cannot do with both 2 for now, =sign alone not working for now
*/
@Suppress("UnstableApiUsage")
internal fun bazelQueryCommandSpec(): ShellCommandSpec = ShellCommandSpec("bazel") {
  subcommands { context: ShellRuntimeContext ->

    subcommand("query") {
      parserOptions = ShellCommandParserOptions.create(optionArgSeparators = listOf("=", " "))
      description("Executes a dependency graph query.")

      allOptions(context)

      // This surrounding is to make terminal still suggests even if we typed 'unknown tokens', e.g. options arguments like integer or comma-seperated
      dummyArgs()

      queryCompletion()

      dummyArgs()
    }
  }
}

@Suppress("UnstableApiUsage")
fun ShellCommandContext.dummyArgs() {
  argument {
    displayName("option")
    isVariadic = true
    isOptional = true
    suggestions(ShellRuntimeDataGenerator { context ->
      listOf(ShellCompletionSuggestion(name = context.typedPrefix, isHidden = true))
    })
  }
}

@Suppress("UnstableApiUsage")
fun ShellCommandContext.queryCompletion() {
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
            name = "${it.name}()",
            description = functionDescriptionHtml(it, context.project),
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
}

@Suppress("UnstableApiUsage")
fun ShellCommandContext.allOptions(context: ShellRuntimeContext) {
  knownOptions.forEach { queryFlag ->
    val flagNameDD = "--${queryFlag.name}"
    val flag = Flag.byName(flagNameDD)
    if (flag != null) {
      // TODO maybe add arguments for some flags
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
            argument {
              suggestions { queryFlag.values.map {
                ShellCompletionSuggestion(it)
              } }
            }
          }
        }

        is Flag.Unknown -> {
          option(flagNameDD)
        }

        else -> {
          optionWithUnknownArgs(flagNameDD)
        }
      }
    }
  }
}

@Suppress("UnstableApiUsage")
fun ShellCommandContext.optionWithUnknownArgs(name: String) {
  option(name) {
    argument {
      isOptional = true
      suggestions {
        listOf(ShellCompletionSuggestion(name = "", isHidden = true))
      }
    }
  }
}

@Suppress("UnstableApiUsage")
fun ShellCommandContext.booleanAndTriStateFlagSuggestion(flag: Flag, context: ShellRuntimeContext) {
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

fun flagDescriptionHtml(flag: Flag,project: Project): String {
  // TODO not this copy paste from BazelFlagDocumentationTarget
  val markdownText =
    """
    |***--${flag.option.name}***
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
  return DocMarkdownToHtmlConverter.convert(project, markdownText)
}

fun functionDescriptionHtml(function: BazelqueryFunction,project: Project): String {
  // TODO not this copy paste from functions
  val markdownText =
    """
    |${function.description}
    |
    |**Arguments:**
    |${function.argumentsMarkdown()}
    |
    |**Example Usage:**
    ```
    ${if (function is BazelqueryFunction.SimpleFunction) function.exampleUsage else "N/A"}
    ```
    """.trimMargin()
  return DocMarkdownToHtmlConverter.convert(project, markdownText)
}

fun isStartAndEndWithQuote(expression: String) : Boolean {
  return expression.length >= 2 && ((expression.startsWith('\'') && expression.endsWith('\'')) || (expression.startsWith('"') && expression.endsWith('"')))
}

val knownCommands = BazelqueryFunction.getAll()

private fun BazelqueryFunction.argumentsMarkdown(): String =
  arguments.joinToString(separator = "\n") { arg ->
    "- `${arg.name}` (${arg.type}${if (arg.optional) ", optional" else ""}): ${arg.description}"
  }

val knownOptions = BazelqueryCommonOptions.getAll()
