package org.jetbrains.bazel.languages.bazelquery.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.startOffset
import com.intellij.util.ProcessingContext
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.languages.bazelquery.BazelQueryLanguage
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenSets
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenType
import org.jetbrains.bazel.languages.bazelquery.psi.BazelQueryFile
import org.jetbrains.bazel.utils.BazelWorkingDirectoryManager
import java.nio.file.Path

class BazelQueryCompletionContributor : CompletionContributor() {
  init {
    extend(CompletionType.BASIC, BazelWordCompletionProvider.psiPattern, BazelWordCompletionProvider())
  }
}

val knownCommands =
  BazelQueryTokenSets.COMMANDS.types
    .map { tokenType ->
      tokenType as BazelQueryTokenType
      tokenType.completionText() + "()"
    }.toList()

val knownOperations =
  BazelQueryTokenSets.OPERATIONS.types
    .map { tokenType ->
      tokenType as BazelQueryTokenType
      tokenType.completionText()
    }.toList()

class BazelQueryPrefixMatcher(prefix: String) : PrefixMatcher(prefix) {
  override fun prefixMatches(name: String): Boolean = name.startsWith(prefix, ignoreCase = true)

  override fun cloneWithPrefix(newPrefix: String): PrefixMatcher = BazelQueryPrefixMatcher(newPrefix)
}

private val wordsOrCommandsTokens =
  TokenSet.create(
    *BazelQueryTokenSets.WORDS.types,
    *BazelQueryTokenSets.COMMANDS.types,
  )

private class BazelWordCompletionProvider : CompletionProvider<CompletionParameters>() {
  private val startTargetSign = "//"

  companion object {
    val psiPattern =
      psiElement()
        .withLanguage(BazelQueryLanguage)
        .and(
          psiElement().inside(psiElement(BazelQueryFile::class.java)),
        ).andOr(
          psiElement().withElementType(wordsOrCommandsTokens),
          psiElement().atStartOf(psiElement().withElementType(wordsOrCommandsTokens)),
        )
  }

  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    val prefix =
      parameters.position.text
        .take(parameters.offset - parameters.position.startOffset)
        .trim()
        .removePrefix("\"")
        .removePrefix("'")
    val currentDirectory = BazelWorkingDirectoryManager.getWorkingDirectory()
    val project = parameters.editor.project ?: return
    val targetSuggestions =
      TargetCompletionsGenerator(project)
        .getTargetsList(prefix, Path.of(currentDirectory))

    result.withPrefixMatcher(BazelQueryPrefixMatcher(prefix)).run {
      addAllElements(
        knownCommands.map { functionLookupElement(it, 1.0) },
      )
      addAllElements(
        knownOperations.map { functionLookupElement(it, 0.0) },
      )
      addAllElements(
        targetSuggestions.map {
          functionLookupElement(
            it,
            when {
              it.startsWith(startTargetSign) &&
                (it.endsWith(":all") || it.endsWith(":all-targets")) -> 0.1

              it.startsWith(startTargetSign) -> 0.3

              !it.startsWith(startTargetSign) &&
                (it.endsWith(":all") || it.endsWith(":all-targets")) -> 0.2

              else -> 0.4
            },
          )
        },
      )
    }
  }

  private fun functionLookupElement(name: String, priority: Double): LookupElement =
    PrioritizedLookupElement.withPriority(
      LookupElementBuilder
        .create(name)
        .withBoldness(true)
        .withIcon(BazelPluginIcons.bazel)
        .withInsertHandler { context, _ ->
          val editor = context.editor
          val caretOffset = editor.caretModel.offset
          if (caretOffset > 0 && editor.document.charsSequence[caretOffset - 1] == ')') {
            editor.caretModel.moveToOffset(caretOffset - 1)
          }
        },
      priority,
    )
}
