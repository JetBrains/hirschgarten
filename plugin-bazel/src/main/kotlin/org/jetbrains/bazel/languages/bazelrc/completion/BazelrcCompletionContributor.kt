package org.jetbrains.bazel.languages.bazelrc.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.AutoCompletionPolicy
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.findParentOfType
import com.intellij.util.ProcessingContext
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.languages.bazelrc.BazelrcLanguage
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes
import org.jetbrains.bazel.languages.bazelrc.flags.BazelFlagSymbol
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.bazel.languages.bazelrc.flags.OptionEffectTag
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcFile
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcFlag
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcLine

class BazelrcCompletionContributor : CompletionContributor() {
  init {
    extend(CompletionType.BASIC, BazelCommandCompletionProvider.psiPattern, BazelCommandCompletionProvider())
    extend(CompletionType.BASIC, BazelImportCompletionProvider.psiPattern, BazelImportCompletionProvider())
    extend(CompletionType.BASIC, BazelConfigCompletionProvider.psiPattern, BazelConfigCompletionProvider())
    extend(CompletionType.BASIC, BazelFlagCompletionProvider.psiPattern, BazelFlagCompletionProvider())
  }

  override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
    super.fillCompletionVariants(parameters, result)
  }
}

val importOrCommandTokens = TokenSet.create(BazelrcTokenTypes.COMMAND, BazelrcTokenTypes.IMPORT)

class BazelCommandCompletionProvider : CompletionProvider<CompletionParameters>() {
  companion object {
    val psiPattern =
      psiElement().withLanguage(BazelrcLanguage).andOr(
        psiElement().withElementType(importOrCommandTokens),
        psiElement().atStartOf(psiElement().withElementType(importOrCommandTokens)),
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
    result.run {
      addAllElements(
        knownCommandsToDescriptions.map { (key, value) -> functionLookupElement(key, value) },
      )
    }
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
    val psiPattern =
      psiElement().withLanguage(BazelrcLanguage).andOr(
        psiElement(BazelrcTokenTypes.CONFIG),
        psiElement().beforeLeaf(psiElement(BazelrcTokenTypes.CONFIG)),
      )
  }

  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    PsiTreeUtil.getParentOfType(parameters.position, BazelrcLine::class.java)?.run {
      val processor = BazelrcConfigDeclarationsProcessor(this)
      processor.processFile(this.containingFile?.originalFile as? BazelrcFile)
      result.addAllElements(processor.results.keys.map { functionLookupElement(it) })
    }
  }

  private fun functionLookupElement(name: String): LookupElement =
    LookupElementBuilder.create(name).withBoldness(true).withIcon(BazelPluginIcons.bazel)
}

class BazelImportCompletionProvider : CompletionProvider<CompletionParameters>() {
  companion object {
    val psiPattern =
      psiElement().withLanguage(BazelrcLanguage).andOr(
        psiElement().withElementType(importOrCommandTokens),
        psiElement().atStartOf(psiElement().withElementType(importOrCommandTokens)),
      )

    val importKeywordAndDescriptions =
      mapOf(
        "import" to "add a required import statement",
        "try-import" to "adds an optional import statement",
      )
  }

  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    result.run {
      addAllElements(
        importKeywordAndDescriptions.map { (keyword, description) ->
          LookupElementBuilder
            .create(keyword)
            .withTailText(" $description", true)
            .withIcon(BazelPluginIcons.bazelConfig)
            .withAutoCompletionPolicy(AutoCompletionPolicy.ALWAYS_AUTOCOMPLETE)
        },
      )
    }
  }
}

class BazelFlagCompletionProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    parameters.position
      .findParentOfType<BazelrcLine>()
      ?.command
      ?.let { command ->
        Flag
          .all()
          .filterNot {
            // filter out old flag names
            it.value.option.oldName
              .isNotEmpty() &&
              it.key.endsWith(it.value.option.oldName)
          }.filterNot {
            // filter out NO_OP flags
            it.value.option.effectTags
              .contains(OptionEffectTag.NO_OP)
          }.filterValues {
            // filter out values that don't apply to the current command line+
            command == "common" || it.option.commands.contains(command)
          }
      }?.let { flagsMap ->
        val pos = parameters.position
        result.run {
          addAllElements(
            flagsMap.entries.map { it -> Pair(it.key, BazelFlagSymbol(it.value, pos.project)) }.map { (k, flagSymbol) ->
              LookupElementBuilder
                .create(flagSymbol, k)
                .withLookupString("${k}_xxx")
                .withTypeText(" ${flagSymbol.flag.option.valueHelp}")
                .withPresentableText(k)
                .withInsertHandler { ctx, _ ->
                  ctx.file.findElementAt(ctx.startOffset)?.let { psiElement ->
                    if (psiElement.text != k) {
                      ctx.editor.document.replaceString(psiElement.textOffset, psiElement.endOffset, k)
                    }
                  }
                }
            },
          )
        }
      }
  }

  companion object {
    val psiPattern =
      psiElement()
        .withElementType(TokenSet.create(BazelrcTokenTypes.VALUE, BazelrcTokenTypes.FLAG))
        .atStartOf(psiElement(BazelrcFlag::class.java))
        .withTreeParent(
          psiElement(BazelrcFlag::class.java),
        )
  }
}

inline fun <T> T.letIf(predicate: Boolean, block: (T) -> T): T =
  if (predicate) {
    block(this)
  } else {
    this
  }
