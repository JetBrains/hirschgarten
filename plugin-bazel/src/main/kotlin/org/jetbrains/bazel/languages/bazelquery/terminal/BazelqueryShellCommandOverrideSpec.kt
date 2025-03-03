package org.jetbrains.bazel.languages.bazelquery.terminal

import com.intellij.terminal.completion.spec.ShellCommandParserOptions
import com.intellij.terminal.completion.spec.ShellCommandSpec
import com.intellij.terminal.completion.spec.ShellRuntimeContext
import org.jetbrains.plugins.terminal.block.completion.spec.*

/*
TODO
- flags
- expressions
 */
internal fun bazelQueryCommandSpec(): ShellCommandSpec = ShellCommandSpec("bazel") {
  requiresSubcommand = true
  subcommands { context: ShellRuntimeContext ->

    subcommand("query") {
      description("Executes a dependency graph query.")

      parserOptions = ShellCommandParserOptions.create(optionArgSeparators = listOf("=", " "))


      argument {
        isVariadic = true
        displayName("query expression")
        suggestions(ShellRuntimeDataGenerator { context: ShellRuntimeContext ->
          if (context.typedPrefix.contains("aa")) {
            listOf(ShellCompletionSuggestion(name = "aaa" + context.shellName.name, prefixReplacementIndex = 1))
          } else {
            listOf(ShellCompletionSuggestion( context.shellName.name + "x"))
          }
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

val targets1 = listOf("target11", "target12")
val targets2 = listOf("target21", "target22")

val targetwithspace = listOf("target 1", "target 2", "target 3")

val functions1 = listOf("fun1", "fun2")

val keywords = listOf("except", "in", "intersect", "let", "set", "union")

