package org.jetbrains.bazel.languages.projectview.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.tree.IElementType
import org.jetbrains.bazel.languages.projectview.elements.ProjectViewElementTypes
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType

class ProjectViewParser: PsiParser {
  private val LOG = Logger.getInstance(ProjectViewParser::class.java)
  private val IMPORT_KEYWORD = "import";

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
      val tokenText = builder.tokenText;
      builder.advanceLexer()
      builder.error("Unexpected token: $tokenText") //TODO(localization)
    }
  }

  fun parseList(builder: PsiBuilder) {
    parseListKeyword(builder);
    parseListEntries(builder);
  }

  fun parseListKeyword(builder: PsiBuilder) {
    LOG.assertTrue(builder.tokenType == ProjectViewTokenType.LIST_KEYWORD)
    val listKeyword = builder.mark();
    builder.advanceLexer()
    listKeyword.done(ProjectViewElementTypes.LIST_KEY)
  }

  fun parseListEntries(builder: PsiBuilder) {
    val listValue = builder.mark();
    while (!builder.eof() && builder.tokenType != ProjectViewTokenType.LIST_KEYWORD &&
      builder.tokenType != ProjectViewTokenType.SCALAR_KEYWORD) {
      builder.advanceLexer();
    }
    listValue.done(ProjectViewElementTypes.LIST_VALUE);
  }

  fun parseScalar(builder: PsiBuilder) {
    val isImport = parseScalarKey(builder)
    if (!isImport) {
      parseColon(builder)
    }
    parseScalarValue(builder)
  }

  fun parseScalarKey(builder: PsiBuilder): Boolean {
    LOG.assertTrue(builder.tokenType == ProjectViewTokenType.IDENTIFIER)
    val key = builder.mark()
    val isImport = builder.tokenText == IMPORT_KEYWORD
    builder.advanceLexer()
    key.done(ProjectViewElementTypes.SCALAR_KEY)
    return isImport
  }

  fun parseScalarValue(builder: PsiBuilder) {
    LOG.assertTrue(builder.tokenType == ProjectViewTokenType.IDENTIFIER)
    val value = builder.mark()
    builder.advanceLexer()
    value.done(ProjectViewElementTypes.SCALAR_VALUE)
  }

  fun parseColon(builder: PsiBuilder) {
    LOG.assertTrue(builder.tokenType == ProjectViewTokenType.COLON)
    builder.advanceLexer()
  }
}
