// This is a generated file. Not intended for manual editing.
package org.jetbrains.bsp.bazel.languages.starlark;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.bsp.bazel.languages.starlark.psi.impl.*;

public interface StarlarkTypes {

  IElementType ARGUMENT = new StarlarkElementType("ARGUMENT");
  IElementType ARGUMENTS = new StarlarkElementType("ARGUMENTS");
  IElementType ASSIGN_STMT = new StarlarkElementType("ASSIGN_STMT");
  IElementType BINARY_EXPR = new StarlarkElementType("BINARY_EXPR");
  IElementType BINARY_EXPR_1 = new StarlarkElementType("BINARY_EXPR_1");
  IElementType BINARY_EXPR_2 = new StarlarkElementType("BINARY_EXPR_2");
  IElementType BINARY_EXPR_21 = new StarlarkElementType("BINARY_EXPR_21");
  IElementType BINOP = new StarlarkElementType("BINOP");
  IElementType BREAK_STMT = new StarlarkElementType("BREAK_STMT");
  IElementType CALL_SUFFIX = new StarlarkElementType("CALL_SUFFIX");
  IElementType COMP_CLAUSE = new StarlarkElementType("COMP_CLAUSE");
  IElementType CONTINUE_STMT = new StarlarkElementType("CONTINUE_STMT");
  IElementType DEF_STMT = new StarlarkElementType("DEF_STMT");
  IElementType DICT_COMP = new StarlarkElementType("DICT_COMP");
  IElementType DICT_EXPR = new StarlarkElementType("DICT_EXPR");
  IElementType DOT_SUFFIX = new StarlarkElementType("DOT_SUFFIX");
  IElementType ENTRIES = new StarlarkElementType("ENTRIES");
  IElementType ENTRY = new StarlarkElementType("ENTRY");
  IElementType EXPRESSION = new StarlarkElementType("EXPRESSION");
  IElementType EXPR_STMT = new StarlarkElementType("EXPR_STMT");
  IElementType FOR_STMT = new StarlarkElementType("FOR_STMT");
  IElementType IF_EXPR = new StarlarkElementType("IF_EXPR");
  IElementType IF_EXPR_1 = new StarlarkElementType("IF_EXPR_1");
  IElementType IF_EXPR_2 = new StarlarkElementType("IF_EXPR_2");
  IElementType IF_STMT = new StarlarkElementType("IF_STMT");
  IElementType LAMBDA_EXPR = new StarlarkElementType("LAMBDA_EXPR");
  IElementType LIST_COMP = new StarlarkElementType("LIST_COMP");
  IElementType LIST_EXPR = new StarlarkElementType("LIST_EXPR");
  IElementType LOAD_STMT = new StarlarkElementType("LOAD_STMT");
  IElementType LOOP_VARIABLES = new StarlarkElementType("LOOP_VARIABLES");
  IElementType OPERAND = new StarlarkElementType("OPERAND");
  IElementType PARAMETER = new StarlarkElementType("PARAMETER");
  IElementType PARAMETERS = new StarlarkElementType("PARAMETERS");
  IElementType PASS_STMT = new StarlarkElementType("PASS_STMT");
  IElementType PRIMARY_EXPR = new StarlarkElementType("PRIMARY_EXPR");
  IElementType PRIMARY_EXPR_1 = new StarlarkElementType("PRIMARY_EXPR_1");
  IElementType RETURN_STMT = new StarlarkElementType("RETURN_STMT");
  IElementType SIMPLE_STMT = new StarlarkElementType("SIMPLE_STMT");
  IElementType SLICE_SUFFIX = new StarlarkElementType("SLICE_SUFFIX");
  IElementType SMALL_STMT = new StarlarkElementType("SMALL_STMT");
  IElementType STATEMENT = new StarlarkElementType("STATEMENT");
  IElementType SUITE = new StarlarkElementType("SUITE");
  IElementType TEST = new StarlarkElementType("TEST");
  IElementType UNARY_EXPR = new StarlarkElementType("UNARY_EXPR");

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
      else if (type == ASSIGN_STMT) {
        return new StarlarkAssignStmtImpl(node);
      }
      else if (type == BINARY_EXPR) {
        return new StarlarkBinaryExprImpl(node);
      }
      else if (type == BINARY_EXPR_1) {
        return new StarlarkBinaryExpr1Impl(node);
      }
      else if (type == BINARY_EXPR_2) {
        return new StarlarkBinaryExpr2Impl(node);
      }
      else if (type == BINARY_EXPR_21) {
        return new StarlarkBinaryExpr21Impl(node);
      }
      else if (type == BINOP) {
        return new StarlarkBinopImpl(node);
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
      else if (type == DEF_STMT) {
        return new StarlarkDefStmtImpl(node);
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
      else if (type == EXPRESSION) {
        return new StarlarkExpressionImpl(node);
      }
      else if (type == EXPR_STMT) {
        return new StarlarkExprStmtImpl(node);
      }
      else if (type == FOR_STMT) {
        return new StarlarkForStmtImpl(node);
      }
      else if (type == IF_EXPR) {
        return new StarlarkIfExprImpl(node);
      }
      else if (type == IF_EXPR_1) {
        return new StarlarkIfExpr1Impl(node);
      }
      else if (type == IF_EXPR_2) {
        return new StarlarkIfExpr2Impl(node);
      }
      else if (type == IF_STMT) {
        return new StarlarkIfStmtImpl(node);
      }
      else if (type == LAMBDA_EXPR) {
        return new StarlarkLambdaExprImpl(node);
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
      else if (type == PRIMARY_EXPR) {
        return new StarlarkPrimaryExprImpl(node);
      }
      else if (type == PRIMARY_EXPR_1) {
        return new StarlarkPrimaryExpr1Impl(node);
      }
      else if (type == RETURN_STMT) {
        return new StarlarkReturnStmtImpl(node);
      }
      else if (type == SIMPLE_STMT) {
        return new StarlarkSimpleStmtImpl(node);
      }
      else if (type == SLICE_SUFFIX) {
        return new StarlarkSliceSuffixImpl(node);
      }
      else if (type == SMALL_STMT) {
        return new StarlarkSmallStmtImpl(node);
      }
      else if (type == STATEMENT) {
        return new StarlarkStatementImpl(node);
      }
      else if (type == SUITE) {
        return new StarlarkSuiteImpl(node);
      }
      else if (type == TEST) {
        return new StarlarkTestImpl(node);
      }
      else if (type == UNARY_EXPR) {
        return new StarlarkUnaryExprImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
