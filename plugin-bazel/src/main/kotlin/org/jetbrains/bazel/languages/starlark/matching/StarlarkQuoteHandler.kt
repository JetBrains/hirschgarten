package org.jetbrains.bazel.languages.starlark.matching

import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets

class StarlarkQuoteHandler : SimpleTokenSetQuoteHandler(StarlarkTokenSets.STRINGS)
