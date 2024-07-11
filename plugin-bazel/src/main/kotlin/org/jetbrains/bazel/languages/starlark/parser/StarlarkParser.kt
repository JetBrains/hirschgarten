package org.jetbrains.bazel.languages.starlark.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType

class StarlarkParser : PsiParser {
  override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
    val rootMarker = builder.mark()
    val context = ParsingContext(builder)
    var lastAfterSemicolon = false

    while (!builder.eof()) {
      context.pushScope(context.emptyParsingScope())
      if (lastAfterSemicolon) {
        context.statementParser.parseSimpleStatement()
      } else {
        context.statementParser.parseStatement()
      }
      lastAfterSemicolon = context.getScope().isAfterSemicolon
      context.popScope()
    }

    rootMarker.done(root)
    return builder.treeBuilt
  }

}
