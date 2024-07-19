package org.jetbrains.bazel.languages.starlark.indentation

import com.intellij.codeInsight.editorActions.enter.EnterBetweenBracesDelegate

class StarlarkEnterBetweenBracesHandler : EnterBetweenBracesDelegate() {
  override fun isBracePair(lBrace: Char, rBrace: Char): Boolean =
      super.isBracePair(lBrace, rBrace) || lBrace == '[' && rBrace == ']'
}
