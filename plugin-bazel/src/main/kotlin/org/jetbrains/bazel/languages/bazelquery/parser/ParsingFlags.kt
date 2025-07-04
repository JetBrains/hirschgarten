package org.jetbrains.bazel.languages.bazelquery.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.psi.tree.IElementType
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryElementTypes
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenSets
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenTypes

open class ParsingFlags(private val root: IElementType, val builder: PsiBuilder) : PsiBuilder by builder {
  private val utils = ParsingUtils(builder)

  fun parseFile(): ASTNode {
    val flagsList = builder.mark()
    while (!eof()) {
      when {
        utils.atAnyToken(BazelQueryTokenSets.FLAGS) -> parseFlag()
        else -> utils.advanceError("unexpected token: query option expected")
      }
    }
    flagsList.done(root)
    return treeBuilt
  }

  private fun parseFlag() {
    val flag = mark()

    if (utils.atToken(BazelQueryTokenTypes.UNFINISHED_FLAG)) {
      utils.advanceError("unfinished flag")
      utils.expectToken(BazelQueryTokenTypes.WHITE_SPACE)
    } else if (utils.atToken(BazelQueryTokenTypes.FLAG)) {
      advanceLexer()

      if (!utils.matchToken(BazelQueryTokenTypes.EQUALS)) {
        utils.advanceError("expected flag value")
      } else {
        if (utils.matchToken(BazelQueryTokenTypes.UNFINISHED_VAL)) {
          error(BazelPluginBundle.message("bazelquery.error.missing.quote"))
        } else if (!utils.matchAnyToken(BazelQueryTokenSets.FLAG_VALS)) {
          error(BazelPluginBundle.message("bazelquery.error.missing.flag"))
        } else if (!eof()) {
          utils.expectToken(BazelQueryTokenTypes.WHITE_SPACE)
        }
      }
    } else {
      advanceLexer()
      while (utils.matchToken(BazelQueryTokenTypes.MISSING_SPACE)) {
        utils.advanceError("<space> expected")
      }
      utils.matchToken(BazelQueryTokenTypes.WHITE_SPACE)
    }

    flag.done(BazelQueryElementTypes.FLAG)
  }
}
