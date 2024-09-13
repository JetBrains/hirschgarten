package org.jetbrains.bazel.languages.bazelrc.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.ProcessingContext
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.languages.bazelrc.BazelrcLanguage
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcFile

class BazelrcCompletionContributor : CompletionContributor() {
  init {
    extend(CompletionType.BASIC, BazelCommandCompletionProvider.psiElementPattern, BazelCommandCompletionProvider())
    extend(CompletionType.BASIC, BazelConfigCompletionProvider.psiElementPattern, BazelConfigCompletionProvider())
  }
}

class BazelCommandCompletionProvider : CompletionProvider<CompletionParameters>() {
  companion object {
    val psiElementPattern =
      psiElement()
        .withLanguage(BazelrcLanguage)
        .andOr(
          psiElement(BazelrcTokenTypes.COMMAND),
          psiElement().beforeLeaf(psiElement(BazelrcTokenTypes.COMMAND)),
        )

    // TODO(BAZEL-1195): Find a nicer way of building this, and the "set of correct flag with type / value for command" ?
    //    Maybe parsing the bazel help output on project sync?
    val knownCommandsToDescriptions =
      mapOf(
        "common" to "flags that apply to all commands",
        "analyze-profile" to "Analyzes build profile data.",
        "aquery" to "Analyzes the given targets and queries the action graph.",
        "build" to "Builds the specified targets.",
        "canonicalize-flags" to "Canonicalizes a list of bazel options.",
        "clean" to "Removes output files and optionally stops the server.",
        "coverage" to "Generates code coverage report for specified test targets.",
        "cquery" to "Loads, analyzes, and queries the specified targets w/ configurations.",
        "dump" to "Dumps the internal state of the bazel server process.",
        "fetch" to "Fetches external repositories that are prerequisites to the targets.",
        "help" to "Prints help for commands, or the index.",
        "info" to "Displays runtime info about the bazel server.",
        "license" to "Prints the license of this software.",
        "mobile-install" to "Installs targets to mobile devices.",
        "mod" to "Queries the Bzlmod external dependency graph",
        "print_action" to "Prints the command line args for compiling a file.",
        "query" to "Executes a dependency graph query.",
        "run" to "Runs the specified target.",
        "shutdown" to "Stops the bazel server.",
        "sync" to "Syncs all repositories specified in the workspace file",
        "test" to "Builds and runs the specified test targets.",
        "vendor" to "Fetches external repositories into a folder specified by the flag --vendor_dir.",
        "version" to "Prints version information for bazel.",
      )
  }

  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    knownCommandsToDescriptions.forEach { result.addElement(functionLookupElement(it.key, it.value)) }
  }

  private fun functionLookupElement(name: String, description: String): LookupElement =
    LookupElementBuilder
      .create(name)
      .withBoldness(true)
      .withTailText(" $description")
      .withIcon(BazelPluginIcons.bazel)
}

private class BazelConfigCompletionProvider : CompletionProvider<CompletionParameters>() {
  companion object {
    val psiElementPattern =
      psiElement()
        .withLanguage(BazelrcLanguage)
        .andOr(
          psiElement(BazelrcTokenTypes.CONFIG),
          psiElement().beforeLeaf(psiElement(BazelrcTokenTypes.CONFIG)),
        )
  }

  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    val rcFile = parameters.originalFile as? BazelrcFile

    rcFile?.findDeclaredConfigs()?.forEach { result.addElement(functionLookupElement(it)) }
  }

  private fun functionLookupElement(name: String): LookupElement =
    LookupElementBuilder
      .create(name)
      .withBoldness(true)
      .withIcon(BazelPluginIcons.bazel)
}
