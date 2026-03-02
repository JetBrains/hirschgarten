package org.jetbrains.bazel.languages.bazelrc.matching

import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenSets

internal class BazelrcQuoteHandler : SimpleTokenSetQuoteHandler(BazelrcTokenSets.BIBI)
