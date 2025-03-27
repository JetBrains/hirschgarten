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
import kotlin.collections.plus

/*
TODO
- flags
- expressions
 */
@Suppress("UnstableApiUsage")
internal fun bazelQueryCommandSpec(): ShellCommandSpec = ShellCommandSpec("bazel") {
  subcommands { context: ShellRuntimeContext ->

    subcommand("query") {
      description("Executes a dependency graph query.")

      parserOptions = ShellCommandParserOptions.create(optionArgSeparators = listOf("="))

      queryCompletion()

      bazelOptions(context)

    }
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
fun ShellCommandContext.bazelOptions(context: ShellRuntimeContext) {
  knownOptions.forEach { queryFlag ->
    val flagNameDD = "--${queryFlag.name}"
    val flag = Flag.byName(flagNameDD)
    if (flag != null) {
      // TODO maybe add arguments for some flags
      when (flag) {
        // Should be done with `separator = "="` and arguments, but currently not working IJPL-150188
        // Cannot hide options
        is Flag.Boolean -> {
          booleanFlagSuggestion(flag, context)
        }

        is Flag.TriState -> {

        }
        else -> {}
      }
    }
  }
}

@Suppress("UnstableApiUsage")
fun ShellCommandContext.booleanFlagSuggestion(flag: Flag, context: ShellRuntimeContext) {
  val preferedTrueFlag = "--${flag.option.name}"
  val trueFlags = listOf("${flag.option.name}=true", "${flag.option.name}=yes", "${flag.option.name}=1")
  val trueFlagsPriority = if (flag.option.defaultValue == "\"true\"") 1 else 50 // Default priority is 50

  val preferedFalseFlag = "--no${flag.option.name}"
  val falseFlags = listOf("${flag.option.name}=false", "${flag.option.name}=no", "${flag.option.name}=0")
  val falseFlagsPriority = 50 - trueFlagsPriority + 1
  // Preferred flags have priority 50 or 1, non-preferred 49 or 0
  val allFlags = trueFlags + preferedTrueFlag + falseFlags + preferedFalseFlag

  generateFlags(preferedTrueFlag, trueFlags, allFlags, trueFlagsPriority, flag, context)
  generateFlags(preferedFalseFlag, falseFlags, allFlags, falseFlagsPriority, flag, context)
}

@Suppress("UnstableApiUsage")
fun ShellCommandContext.generateFlags(preferedFlag: String, remainingFlags: List<String>, allFlags: List<String>, prio: Int, flag: Flag, context: ShellRuntimeContext) {
  option(preferedFlag) {
    description(flagDescriptionHtml(flag,context.project))
    priority = prio
    exclusiveOn = allFlags
  }
  option(*remainingFlags.toTypedArray()) {
    description(flagDescriptionHtml(flag,context.project))
    priority = prio - 1
    exclusiveOn = allFlags
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

val knownOptions = BazelqueryCommonOptions().getAll()
