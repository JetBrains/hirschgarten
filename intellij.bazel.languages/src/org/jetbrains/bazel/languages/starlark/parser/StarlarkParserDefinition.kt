package org.jetbrains.bazel.languages.starlark.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets
import org.jetbrains.bazel.languages.starlark.lexer.StarlarkIndentingLexer
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.ARGUMENT_EXPRESSION
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.ARGUMENT_LIST
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.ASSIGNMENT_STATEMENT
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.AUG_ASSIGNMENT_STATEMENT
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.BINARY_EXPRESSION
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.BREAK_STATEMENT
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.CALL_EXPRESSION
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.CONDITIONAL_EXPRESSION
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.CONTINUE_STATEMENT
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.DICT_COMP_EXPRESSION
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.DICT_LITERAL_EXPRESSION
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.DOUBLE_STAR_EXPRESSION
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.EXPRESSION_STATEMENT
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.FALSE_LITERAL_EXPRESSION
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.FILENAME_LOAD_VALUE
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.FLOAT_LITERAL_EXPRESSION
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.FOR_STATEMENT
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.FUNCTION_DECLARATION
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.GENERATOR_EXPRESSION
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.IF_STATEMENT
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.INTEGER_LITERAL_EXPRESSION
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.KEYWORD_ONLY_BOUNDARY
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.KEYWORD_VARIADIC_PARAMETER
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.KEY_VALUE_EXPRESSION
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.LAMBDA_EXPRESSION
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.LIST_COMP_EXPRESSION
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.LIST_LITERAL_EXPRESSION
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.LOAD_STATEMENT
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.MANDATORY_PARAMETER
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.NAMED_ARGUMENT_EXPRESSION
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.NAMED_LOAD_VALUE
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.NONE_LITERAL_EXPRESSION
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.OPTIONAL_PARAMETER
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.PARAMETER_LIST
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.PARENTHESIZED_EXPRESSION
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.PASS_STATEMENT
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.PREFIX_EXPRESSION
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.REFERENCE_EXPRESSION
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.RETURN_STATEMENT
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.SLICE_EXPRESSION
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.STAR_ARGUMENT_EXPRESSION
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.STAR_EXPRESSION
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.STATEMENT_LIST
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.STRING_LITERAL_EXPRESSION
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.STRING_LOAD_VALUE
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.SUBSCRIPTION_EXPRESSION
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.TRUE_LITERAL_EXPRESSION
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.TUPLE_EXPRESSION
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes.VARIADIC_PARAMETER
import org.jetbrains.bazel.languages.starlark.parser.generated._StarlarkParser
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkBinaryExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkConditionalExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkDictCompExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkDictLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkDoubleStarExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkFalseLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkFloatLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGeneratorExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkIntegerLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkKeyValueExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkLambdaExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListCompExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkNoneLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkPrefixExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkSliceExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStarExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkSubscriptionExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTrueLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTupleExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkStarArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkKeywordOnlyBoundary
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkKeywordVariadicParameter
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkMandatoryParameter
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkOptionalParameter
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkParameterList
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkVariadicParameter
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAugAssignmentStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkBreakStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkContinueStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkExpressionStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkFilenameLoadValue
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkForStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkIfStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkNamedLoadValue
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkPassStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkReturnStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStatementList
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStringLoadValue

@ApiStatus.Internal
class StarlarkParserDefinition : ParserDefinition {
  override fun createLexer(project: Project?): Lexer = StarlarkIndentingLexer()

  override fun createParser(project: Project?): PsiParser = _StarlarkParser()

  override fun getFileNodeType(): IFileElementType = file

  override fun getWhitespaceTokens(): TokenSet = StarlarkTokenSets.WHITESPACE

  override fun getCommentTokens(): TokenSet = StarlarkTokenSets.COMMENT

  override fun getStringLiteralElements(): TokenSet = StarlarkTokenSets.STRINGS

  override fun createElement(node: ASTNode): PsiElement =
    when (val type = node.elementType) {
      ASSIGNMENT_STATEMENT -> StarlarkAssignmentStatement(node)
      AUG_ASSIGNMENT_STATEMENT -> StarlarkAugAssignmentStatement(node)
      BREAK_STATEMENT -> StarlarkBreakStatement(node)
      CONTINUE_STATEMENT -> StarlarkContinueStatement(node)
      EXPRESSION_STATEMENT -> StarlarkExpressionStatement(node)
      FOR_STATEMENT -> StarlarkForStatement(node)
      IF_STATEMENT -> StarlarkIfStatement(node)
      LOAD_STATEMENT -> StarlarkLoadStatement(node)
      FILENAME_LOAD_VALUE -> StarlarkFilenameLoadValue(node)
      NAMED_LOAD_VALUE -> StarlarkNamedLoadValue(node)
      STRING_LOAD_VALUE -> StarlarkStringLoadValue(node)
      PASS_STATEMENT -> StarlarkPassStatement(node)
      RETURN_STATEMENT -> StarlarkReturnStatement(node)
      STATEMENT_LIST -> StarlarkStatementList(node)

      FUNCTION_DECLARATION -> StarlarkFunctionDeclaration(node)
      MANDATORY_PARAMETER -> StarlarkMandatoryParameter(node)
      OPTIONAL_PARAMETER -> StarlarkOptionalParameter(node)
      VARIADIC_PARAMETER -> StarlarkVariadicParameter(node)
      KEYWORD_VARIADIC_PARAMETER -> StarlarkKeywordVariadicParameter(node)
      PARAMETER_LIST -> StarlarkParameterList(node)
      KEYWORD_ONLY_BOUNDARY -> StarlarkKeywordOnlyBoundary(node)

      ARGUMENT_EXPRESSION -> StarlarkArgumentExpression(node)
      ARGUMENT_LIST -> StarlarkArgumentList(node)
      BINARY_EXPRESSION -> StarlarkBinaryExpression(node)
      CALL_EXPRESSION -> StarlarkCallExpression(node)
      CONDITIONAL_EXPRESSION -> StarlarkConditionalExpression(node)
      DICT_COMP_EXPRESSION -> StarlarkDictCompExpression(node)
      DICT_LITERAL_EXPRESSION -> StarlarkDictLiteralExpression(node)
      DOUBLE_STAR_EXPRESSION -> StarlarkDoubleStarExpression(node)
      FALSE_LITERAL_EXPRESSION -> StarlarkFalseLiteralExpression(node)
      FLOAT_LITERAL_EXPRESSION -> StarlarkFloatLiteralExpression(node)
      GENERATOR_EXPRESSION -> StarlarkGeneratorExpression(node)
      INTEGER_LITERAL_EXPRESSION -> StarlarkIntegerLiteralExpression(node)
      KEY_VALUE_EXPRESSION -> StarlarkKeyValueExpression(node)
      LAMBDA_EXPRESSION -> StarlarkLambdaExpression(node)
      LIST_COMP_EXPRESSION -> StarlarkListCompExpression(node)
      LIST_LITERAL_EXPRESSION -> StarlarkListLiteralExpression(node)
      NAMED_ARGUMENT_EXPRESSION -> StarlarkNamedArgumentExpression(node)
      NONE_LITERAL_EXPRESSION -> StarlarkNoneLiteralExpression(node)
      PARENTHESIZED_EXPRESSION -> StarlarkParenthesizedExpression(node)
      PREFIX_EXPRESSION -> StarlarkPrefixExpression(node)
      REFERENCE_EXPRESSION -> StarlarkReferenceExpression(node)
      SLICE_EXPRESSION -> StarlarkSliceExpression(node)
      STAR_ARGUMENT_EXPRESSION -> StarlarkStarArgumentExpression(node)
      STAR_EXPRESSION -> StarlarkStarExpression(node)
      STRING_LITERAL_EXPRESSION -> StarlarkStringLiteralExpression(node)
      SUBSCRIPTION_EXPRESSION -> StarlarkSubscriptionExpression(node)
      TRUE_LITERAL_EXPRESSION -> StarlarkTrueLiteralExpression(node)
      TUPLE_EXPRESSION -> StarlarkTupleExpression(node)

      else -> error("Unknown element type: $type")
    }

  override fun createFile(viewProvider: FileViewProvider): PsiFile = StarlarkFile(viewProvider)
}

private val file = IFileElementType(StarlarkLanguage)
