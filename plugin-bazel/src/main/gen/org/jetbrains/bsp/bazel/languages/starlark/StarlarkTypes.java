// This is a generated file. Not intended for manual editing.
package org.jetbrains.bsp.bazel.languages.starlark;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.bsp.bazel.languages.starlark.psi.impl.*;

public interface StarlarkTypes {

  IElementType ARGUMENT = new StarlarkElementType("ARGUMENT");
  IElementType ARGUMENTS = new StarlarkElementType("ARGUMENTS");
  IElementType ASSIGN_OPERATOR = new StarlarkElementType("ASSIGN_OPERATOR");
  IElementType ASSIGN_STMT = new StarlarkElementType("ASSIGN_STMT");
  IElementType BINARY_EXPRESSION = new StarlarkElementType("BINARY_EXPRESSION");
  IElementType BINARY_OPERATOR = new StarlarkElementType("BINARY_OPERATOR");
  IElementType BREAK_STMT = new StarlarkElementType("BREAK_STMT");
  IElementType CALL_SUFFIX = new StarlarkElementType("CALL_SUFFIX");
  IElementType COMP_CLAUSE = new StarlarkElementType("COMP_CLAUSE");
  IElementType CONTINUE_STMT = new StarlarkElementType("CONTINUE_STMT");
  IElementType DEF_STATEMENT = new StarlarkElementType("DEF_STATEMENT");
  IElementType DICT_COMP = new StarlarkElementType("DICT_COMP");
  IElementType DICT_EXPR = new StarlarkElementType("DICT_EXPR");
  IElementType DOT_SUFFIX = new StarlarkElementType("DOT_SUFFIX");
  IElementType ENTRIES = new StarlarkElementType("ENTRIES");
  IElementType ENTRY = new StarlarkElementType("ENTRY");
  IElementType EXPRESSION = new StarlarkElementType("EXPRESSION");
  IElementType EXPR_STMT = new StarlarkElementType("EXPR_STMT");
  IElementType FOR_STATEMENT = new StarlarkElementType("FOR_STATEMENT");
  IElementType IF_EXPRESSION = new StarlarkElementType("IF_EXPRESSION");
  IElementType IF_LAST_ENTRY = new StarlarkElementType("IF_LAST_ENTRY");
  IElementType IF_LAST_EXPR = new StarlarkElementType("IF_LAST_EXPR");
  IElementType IF_STATEMENT = new StarlarkElementType("IF_STATEMENT");
  IElementType LAMBDA_EXPRESSION = new StarlarkElementType("LAMBDA_EXPRESSION");
  IElementType LIST_COMP = new StarlarkElementType("LIST_COMP");
  IElementType LIST_EXPR = new StarlarkElementType("LIST_EXPR");
  IElementType LOAD_STMT = new StarlarkElementType("LOAD_STMT");
  IElementType LOOP_VARIABLES = new StarlarkElementType("LOOP_VARIABLES");
  IElementType OPERAND = new StarlarkElementType("OPERAND");
  IElementType PARAMETER = new StarlarkElementType("PARAMETER");
  IElementType PARAMETERS = new StarlarkElementType("PARAMETERS");
  IElementType PASS_STMT = new StarlarkElementType("PASS_STMT");
  IElementType PRIMARY_EXPRESSION = new StarlarkElementType("PRIMARY_EXPRESSION");
  IElementType RETURN_STMT = new StarlarkElementType("RETURN_STMT");
  IElementType SIMPLE_STATEMENT = new StarlarkElementType("SIMPLE_STATEMENT");
  IElementType SLICE_SUFFIX = new StarlarkElementType("SLICE_SUFFIX");
  IElementType STATEMENT = new StarlarkElementType("STATEMENT");
  IElementType STMT = new StarlarkElementType("STMT");
  IElementType UNARY_EXPRESSION = new StarlarkElementType("UNARY_EXPRESSION");
  IElementType UNARY_OPERATOR = new StarlarkElementType("UNARY_OPERATOR");

  IElementType AND = new StarlarkTokenType("and");
  IElementType AND_EQ = new StarlarkTokenType("&=");
  IElementType ASTERISK = new StarlarkTokenType("*");
  IElementType ASTERISK_EQ = new StarlarkTokenType("*=");
  IElementType BIT_AND = new StarlarkTokenType("&");
  IElementType BIT_OR = new StarlarkTokenType("|");
  IElementType BIT_SHIFT_LEFT = new StarlarkTokenType("<<");
  IElementType BIT_SHIFT_RIGHT = new StarlarkTokenType(">>");
  IElementType BIT_XOR = new StarlarkTokenType("^");
  IElementType BREAK = new StarlarkTokenType("break");
  IElementType BYTES = new StarlarkTokenType("BYTES");
  IElementType COLON = new StarlarkTokenType(":");
  IElementType COMMA = new StarlarkTokenType(",");
  IElementType COMMENT = new StarlarkTokenType("COMMENT");
  IElementType CONTINUE = new StarlarkTokenType("continue");
  IElementType DEF = new StarlarkTokenType("def");
  IElementType DIV = new StarlarkTokenType("/");
  IElementType DIV_EQ = new StarlarkTokenType("/=");
  IElementType DIV_INT = new StarlarkTokenType("//");
  IElementType DIV_INT_EQ = new StarlarkTokenType("//=");
  IElementType DOT = new StarlarkTokenType(".");
  IElementType DOUBLE_ASTERISK = new StarlarkTokenType("**");
  IElementType ELIF = new StarlarkTokenType("elif");
  IElementType ELSE = new StarlarkTokenType("else");
  IElementType EQ = new StarlarkTokenType("=");
  IElementType FLOAT = new StarlarkTokenType("FLOAT");
  IElementType FOR = new StarlarkTokenType("for");
  IElementType GREATER_OR_EQUAL = new StarlarkTokenType(">=");
  IElementType GREATER_THAN = new StarlarkTokenType(">");
  IElementType IDENTIFIER = new StarlarkTokenType("IDENTIFIER");
  IElementType IF = new StarlarkTokenType("if");
  IElementType IN = new StarlarkTokenType("in");
  IElementType INT = new StarlarkTokenType("INT");
  IElementType IS_EQUAL = new StarlarkTokenType("==");
  IElementType IS_NOT_EQUAL = new StarlarkTokenType("!=");
  IElementType LAMBDA = new StarlarkTokenType("lambda");
  IElementType LEFT_BRACKET = new StarlarkTokenType("[");
  IElementType LEFT_CURLY = new StarlarkTokenType("{");
  IElementType LEFT_PAREN = new StarlarkTokenType("(");
  IElementType LESS_OR_EQUAL = new StarlarkTokenType("<=");
  IElementType LESS_THAN = new StarlarkTokenType("<");
  IElementType LOAD = new StarlarkTokenType("load");
  IElementType MINUS = new StarlarkTokenType("-");
  IElementType MINUS_EQ = new StarlarkTokenType("-=");
  IElementType MODULO = new StarlarkTokenType("%");
  IElementType MODULO_EQ = new StarlarkTokenType("%=");
  IElementType NOT = new StarlarkTokenType("not");
  IElementType OR = new StarlarkTokenType("or");
  IElementType OR_EQ = new StarlarkTokenType("|=");
  IElementType PASS = new StarlarkTokenType("pass");
  IElementType PLUS = new StarlarkTokenType("+");
  IElementType PLUS_EQ = new StarlarkTokenType("+=");
  IElementType RETURN = new StarlarkTokenType("return");
  IElementType RIGHT_BRACKET = new StarlarkTokenType("]");
  IElementType RIGHT_CURLY = new StarlarkTokenType("}");
  IElementType RIGHT_PAREN = new StarlarkTokenType(")");
  IElementType SEMICOLON = new StarlarkTokenType(";");
  IElementType SHIFT_LEFT_EQ = new StarlarkTokenType("<<=");
  IElementType SHIFT_RIGHT_EQ = new StarlarkTokenType(">>=");
  IElementType STRING = new StarlarkTokenType("STRING");
  IElementType TYLDA = new StarlarkTokenType("~");
  IElementType XOR_EQ = new StarlarkTokenType("^=");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == ARGUMENT) {
        return new StarlarkArgumentImpl(node);
      }
      else if (type == ARGUMENTS) {
        return new StarlarkArgumentsImpl(node);
      }
      else if (type == ASSIGN_OPERATOR) {
        return new StarlarkAssignOperatorImpl(node);
      }
      else if (type == ASSIGN_STMT) {
        return new StarlarkAssignStmtImpl(node);
      }
      else if (type == BINARY_EXPRESSION) {
        return new StarlarkBinaryExpressionImpl(node);
      }
      else if (type == BINARY_OPERATOR) {
        return new StarlarkBinaryOperatorImpl(node);
      }
      else if (type == BREAK_STMT) {
        return new StarlarkBreakStmtImpl(node);
      }
      else if (type == CALL_SUFFIX) {
        return new StarlarkCallSuffixImpl(node);
      }
      else if (type == COMP_CLAUSE) {
        return new StarlarkCompClauseImpl(node);
      }
      else if (type == CONTINUE_STMT) {
        return new StarlarkContinueStmtImpl(node);
      }
      else if (type == DEF_STATEMENT) {
        return new StarlarkDefStatementImpl(node);
      }
      else if (type == DICT_COMP) {
        return new StarlarkDictCompImpl(node);
      }
      else if (type == DICT_EXPR) {
        return new StarlarkDictExprImpl(node);
      }
      else if (type == DOT_SUFFIX) {
        return new StarlarkDotSuffixImpl(node);
      }
      else if (type == ENTRIES) {
        return new StarlarkEntriesImpl(node);
      }
      else if (type == ENTRY) {
        return new StarlarkEntryImpl(node);
      }
      else if (type == EXPR_STMT) {
        return new StarlarkExprStmtImpl(node);
      }
      else if (type == FOR_STATEMENT) {
        return new StarlarkForStatementImpl(node);
      }
      else if (type == IF_EXPRESSION) {
        return new StarlarkIfExpressionImpl(node);
      }
      else if (type == IF_LAST_ENTRY) {
        return new StarlarkIfLastEntryImpl(node);
      }
      else if (type == IF_LAST_EXPR) {
        return new StarlarkIfLastExprImpl(node);
      }
      else if (type == IF_STATEMENT) {
        return new StarlarkIfStatementImpl(node);
      }
      else if (type == LAMBDA_EXPRESSION) {
        return new StarlarkLambdaExpressionImpl(node);
      }
      else if (type == LIST_COMP) {
        return new StarlarkListCompImpl(node);
      }
      else if (type == LIST_EXPR) {
        return new StarlarkListExprImpl(node);
      }
      else if (type == LOAD_STMT) {
        return new StarlarkLoadStmtImpl(node);
      }
      else if (type == LOOP_VARIABLES) {
        return new StarlarkLoopVariablesImpl(node);
      }
      else if (type == OPERAND) {
        return new StarlarkOperandImpl(node);
      }
      else if (type == PARAMETER) {
        return new StarlarkParameterImpl(node);
      }
      else if (type == PARAMETERS) {
        return new StarlarkParametersImpl(node);
      }
      else if (type == PASS_STMT) {
        return new StarlarkPassStmtImpl(node);
      }
      else if (type == PRIMARY_EXPRESSION) {
        return new StarlarkPrimaryExpressionImpl(node);
      }
      else if (type == RETURN_STMT) {
        return new StarlarkReturnStmtImpl(node);
      }
      else if (type == SIMPLE_STATEMENT) {
        return new StarlarkSimpleStatementImpl(node);
      }
      else if (type == SLICE_SUFFIX) {
        return new StarlarkSliceSuffixImpl(node);
      }
      else if (type == UNARY_EXPRESSION) {
        return new StarlarkUnaryExpressionImpl(node);
      }
      else if (type == UNARY_OPERATOR) {
        return new StarlarkUnaryOperatorImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
