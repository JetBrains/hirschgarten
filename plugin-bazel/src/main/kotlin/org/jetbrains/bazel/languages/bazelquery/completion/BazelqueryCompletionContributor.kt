package org.jetbrains.bazel.languages.bazelquery.completion

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
import org.jetbrains.bazel.languages.bazelquery.BazelqueryLanguage
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenSets
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.bazel.languages.bazelquery.psi.BazelqueryQueryVal
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import org.jetbrains.plugins.bsp.target.temporaryTargetUtils


class BazelqueryCompletionContributor : CompletionContributor() {
  init {
    extend(CompletionType.BASIC, BazelWordCompletionProvider.psiPattern, BazelWordCompletionProvider())
  }
}

val wordsOrCommandsTokens = TokenSet.create(
  *BazelqueryTokenSets.WORDS.types,
  *BazelqueryTokenSets.COMMANDS.types,
  )

private class BazelWordCompletionProvider : CompletionProvider<CompletionParameters>() {
  companion object {
    val psiPattern =
      psiElement()
        .withLanguage(BazelqueryLanguage)
        .and(
          psiElement().inside(psiElement(BazelqueryQueryVal::class.java))
        )
        .andOr(
          psiElement().withElementType(wordsOrCommandsTokens),
          psiElement().atStartOf(psiElement().withElementType(wordsOrCommandsTokens)),
        )
  }

  val knownCommands =
    BazelqueryTokenSets.COMMANDS.types.map { tokenType ->
      tokenType as BazelqueryTokenType
      tokenType.completionText() + "()"
    }.toList()

  val project: Project? = ProjectManager.getInstance().openProjects.firstOrNull()
  val temporaryTargetUtils = project?.temporaryTargetUtils
  val targetsIds = temporaryTargetUtils?.allTargetIds()
  val targets = targetsIds?.mapNotNull { targetId ->
    temporaryTargetUtils?.getBuildTargetInfoForId(targetId)?.displayName
  }

  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {


    result.run {
      addAllElements(
        knownCommands.map(::functionLookupElement),
      )
      addAllElements(
        targets?.map(::functionLookupElement) ?: emptyList(),
      )
      // TODO: prioritize targets
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
