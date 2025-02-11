package org.jetbrains.bazel.languages.projectview.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.tree.IElementType
import org.jetbrains.bazel.languages.projectview.elements.ProjectViewElementTypes
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType

class ProjectViewParser : PsiParser {
  private val log = Logger.getInstance(ProjectViewParser::class.java)
  private val importKeyword = "import"

  override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
    val rootMarker = builder.mark()

    while (!builder.eof()) {
      parseSection(builder)
    }

    rootMarker.done(root)
    return builder.treeBuilt
  }

  fun parseSection(builder: PsiBuilder) {
    if (builder.tokenType == ProjectViewTokenType.LIST_KEYWORD) {
      val list = builder.mark()
      parseList(builder)
      list.done(ProjectViewElementTypes.LIST)
    } else if (builder.tokenType == ProjectViewTokenType.SCALAR_KEYWORD) {
      val scalar = builder.mark()
      parseScalar(builder)
      scalar.done(ProjectViewElementTypes.SCALAR)
    } else {
      val tokenText = builder.tokenText
      builder.advanceLexer()
      builder.error("Unexpected token: $tokenText")
    }
  }

  fun parseList(builder: PsiBuilder) {
    parseListKeyword(builder)
    parseListEntries(builder)
  }

  fun parseListKeyword(builder: PsiBuilder) {
    log.assertTrue(builder.tokenType == ProjectViewTokenType.LIST_KEYWORD)
    val listKeyword = builder.mark()
    builder.advanceLexer()
    listKeyword.done(ProjectViewElementTypes.LIST_KEY)
  }

  fun parseListEntries(builder: PsiBuilder) {
    while (!builder.eof() &&
      builder.tokenType != ProjectViewTokenType.LIST_KEYWORD &&
      builder.tokenType != ProjectViewTokenType.SCALAR_KEYWORD
    ) {
      if (builder.tokenType == ProjectViewTokenType.IDENTIFIER) {
        val listEntry = builder.mark()
        builder.advanceLexer()
        if (builder.tokenType == ProjectViewTokenType.COLON) {
          parseColon(builder)
          if (builder.tokenType == ProjectViewTokenType.IDENTIFIER) {
            builder.advanceLexer()
          }
        }
        listEntry.done(ProjectViewElementTypes.LIST_VALUE)
      } else {
        builder.advanceLexer()
      }
    }
  }

  fun parseScalar(builder: PsiBuilder) {
    val isImport = parseScalarKey(builder)
    if (!isImport) {
      parseColon(builder)
    }
    parseScalarValue(builder)
  }

  fun parseScalarKey(builder: PsiBuilder): Boolean {
    log.assertTrue(builder.tokenType == ProjectViewTokenType.SCALAR_KEYWORD)
    val key = builder.mark()
    val isImport = builder.tokenText == importKeyword
    builder.advanceLexer()
    key.done(ProjectViewElementTypes.SCALAR_KEY)
    return isImport
  }

  fun parseScalarValue(builder: PsiBuilder) {
    log.assertTrue(builder.tokenType == ProjectViewTokenType.IDENTIFIER)
    val value = builder.mark()
    builder.advanceLexer()
    value.done(ProjectViewElementTypes.SCALAR_VALUE)
  }

  fun parseColon(builder: PsiBuilder) {
    log.assertTrue(builder.tokenType == ProjectViewTokenType.COLON)
    builder.advanceLexer()
  }
}
