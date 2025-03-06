package org.jetbrains.bazel.languages.bazelquery.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.ProcessingContext
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.languages.bazelquery.BazelqueryLanguage
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenSets
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.bazel.languages.bazelquery.psi.BazelqueryQueryVal
import com.intellij.psi.util.startOffset
import org.jetbrains.bazel.languages.bazelquery.psi.BazelqueryFile


class BazelqueryCompletionContributor : CompletionContributor() {
  init {
    extend(CompletionType.BASIC, BazelWordCompletionProvider.psiPattern, BazelWordCompletionProvider())
  }
}

val knownCommands =
  BazelqueryTokenSets.COMMANDS.types.map { tokenType ->
    tokenType as BazelqueryTokenType
    tokenType.completionText() + "()"
  }.toList()

class BazelqueryPrefixMatcher(prefix: String) : PrefixMatcher(prefix) {

  override fun prefixMatches(name: String): Boolean {
    return name.startsWith(prefix, ignoreCase = true)
  }

  override fun cloneWithPrefix(newPrefix: String): PrefixMatcher {
    return BazelqueryPrefixMatcher(newPrefix)
  }
}

private val wordsOrCommandsTokens = TokenSet.create(
  *BazelqueryTokenSets.WORDS.types,
  *BazelqueryTokenSets.COMMANDS.types,
)

private class BazelWordCompletionProvider : CompletionProvider<CompletionParameters>() {
  companion object {
    val psiPattern =
      psiElement()
        .withLanguage(BazelqueryLanguage)
        .and(
          psiElement().inside(psiElement(BazelqueryFile::class.java))
        )
        .andOr(
          psiElement().withElementType(wordsOrCommandsTokens),
          psiElement().atStartOf(psiElement().withElementType(wordsOrCommandsTokens)),
        )
  }

  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    val prefix = parameters.position.text
      .take(parameters.offset - parameters.position.startOffset)
      .trim()
      .removePrefix("\"")
      .removePrefix("'")
    val targetSuggestions = generateTargetCompletions(prefix)

    result.withPrefixMatcher(BazelqueryPrefixMatcher(prefix)).run {
      addAllElements(
        knownCommands.map(::functionLookupElement),
      )
      addAllElements(
        targetSuggestions.map(::functionLookupElement)
      )
    }
  }

  private fun functionLookupElement(name: String): LookupElement =
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
      }
}
