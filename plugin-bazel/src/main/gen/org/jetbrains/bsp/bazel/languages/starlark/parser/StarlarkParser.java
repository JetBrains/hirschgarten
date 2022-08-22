// This is a generated file. Not intended for manual editing.
package org.jetbrains.bsp.bazel.languages.starlark.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static org.jetbrains.bsp.bazel.languages.starlark.StarlarkTypes.*;
import static org.jetbrains.bsp.bazel.languages.starlark.StarlarkParserUtils.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class StarlarkParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, null);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    r = parse_root_(t, b);
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b) {
    return parse_root_(t, b, 0);
  }

  static boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return File(b, l + 1);
  }

  /* ********************************************************** */
  // IDENTIFIER '=' Test
  //             | '*' Test
  //             | '**' Test
  //             | Test
  public static boolean Argument(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Argument")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ARGUMENT, "<argument>");
    r = Argument_0(b, l + 1);
    if (!r) r = Argument_1(b, l + 1);
    if (!r) r = Argument_2(b, l + 1);
    if (!r) r = Test(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // IDENTIFIER '=' Test
  private static boolean Argument_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Argument_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, IDENTIFIER, EQ);
    r = r && Test(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // '*' Test
  private static boolean Argument_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Argument_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ASTERISK);
    r = r && Test(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // '**' Test
  private static boolean Argument_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Argument_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, DOUBLE_ASTERISK);
    r = r && Test(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // Argument (',' Argument)*
  public static boolean Arguments(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Arguments")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ARGUMENTS, "<arguments>");
    r = Argument(b, l + 1);
    r = r && Arguments_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (',' Argument)*
  private static boolean Arguments_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Arguments_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!Arguments_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "Arguments_1", c)) break;
    }
    return true;
  }

  // ',' Argument
  private static boolean Arguments_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Arguments_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && Argument(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // Expression <<noNewLineOrLineBreak>> ('=' | '+=' | '-=' | '*=' | '/=' | '//=' | '%=' | '&=' | '|=' | '^=' | '<<=' | '>>=') <<noNewLineOrLineBreak>> Expression
  public static boolean AssignStmt(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "AssignStmt")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ASSIGN_STMT, "<assign stmt>");
    r = Expression(b, l + 1);
    r = r && noNewLineOrLineBreak(b, l + 1);
    r = r && AssignStmt_2(b, l + 1);
    r = r && noNewLineOrLineBreak(b, l + 1);
    r = r && Expression(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // '=' | '+=' | '-=' | '*=' | '/=' | '//=' | '%=' | '&=' | '|=' | '^=' | '<<=' | '>>='
  private static boolean AssignStmt_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "AssignStmt_2")) return false;
    boolean r;
    r = consumeToken(b, EQ);
    if (!r) r = consumeToken(b, PLUS_EQ);
    if (!r) r = consumeToken(b, MINUS_EQ);
    if (!r) r = consumeToken(b, ASTERISK_EQ);
    if (!r) r = consumeToken(b, DIV_EQ);
    if (!r) r = consumeToken(b, DIV_INT_EQ);
    if (!r) r = consumeToken(b, MODULO_EQ);
    if (!r) r = consumeToken(b, AND_EQ);
    if (!r) r = consumeToken(b, OR_EQ);
    if (!r) r = consumeToken(b, XOR_EQ);
    if (!r) r = consumeToken(b, SHIFT_LEFT_EQ);
    if (!r) r = consumeToken(b, SHIFT_RIGHT_EQ);
    return r;
  }

  /* ********************************************************** */
  // BinaryExpr1 (<<noNewLineOrLineBreak>> Binop <<noNewLineOrLineBreak>> BinaryExpr)?
  public static boolean BinaryExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "BinaryExpr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, BINARY_EXPR, "<binary expr>");
    r = BinaryExpr1(b, l + 1);
    r = r && BinaryExpr_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (<<noNewLineOrLineBreak>> Binop <<noNewLineOrLineBreak>> BinaryExpr)?
  private static boolean BinaryExpr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "BinaryExpr_1")) return false;
    BinaryExpr_1_0(b, l + 1);
    return true;
  }

  // <<noNewLineOrLineBreak>> Binop <<noNewLineOrLineBreak>> BinaryExpr
  private static boolean BinaryExpr_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "BinaryExpr_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = noNewLineOrLineBreak(b, l + 1);
    r = r && Binop(b, l + 1);
    r = r && noNewLineOrLineBreak(b, l + 1);
    r = r && BinaryExpr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // PrimaryExpr
  //               | UnaryExpr
  //               | LambdaExpr
  //               | IfExpr
  public static boolean BinaryExpr1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "BinaryExpr1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, BINARY_EXPR_1, "<binary expr 1>");
    r = PrimaryExpr(b, l + 1);
    if (!r) r = UnaryExpr(b, l + 1);
    if (!r) r = LambdaExpr(b, l + 1);
    if (!r) r = IfExpr(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // BinaryExpr21 (<<noNewLineOrLineBreak>> Binop <<noNewLineOrLineBreak>> BinaryExpr2)?
  public static boolean BinaryExpr2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "BinaryExpr2")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, BINARY_EXPR_2, "<binary expr 2>");
    r = BinaryExpr21(b, l + 1);
    r = r && BinaryExpr2_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (<<noNewLineOrLineBreak>> Binop <<noNewLineOrLineBreak>> BinaryExpr2)?
  private static boolean BinaryExpr2_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "BinaryExpr2_1")) return false;
    BinaryExpr2_1_0(b, l + 1);
    return true;
  }

  // <<noNewLineOrLineBreak>> Binop <<noNewLineOrLineBreak>> BinaryExpr2
  private static boolean BinaryExpr2_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "BinaryExpr2_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = noNewLineOrLineBreak(b, l + 1);
    r = r && Binop(b, l + 1);
    r = r && noNewLineOrLineBreak(b, l + 1);
    r = r && BinaryExpr2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // PrimaryExpr
  //                | UnaryExpr
  //                | LambdaExpr
  public static boolean BinaryExpr21(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "BinaryExpr21")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, BINARY_EXPR_21, "<binary expr 21>");
    r = PrimaryExpr(b, l + 1);
    if (!r) r = UnaryExpr(b, l + 1);
    if (!r) r = LambdaExpr(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // or | and
  //       | '==' | '!=' | '<' | '>' | '<=' | '>=' | in | not in
  //       | '|'
  //       | '^'
  //       | '&'
  //       | '<<' | '>>'
  //       | '-' | '+'
  //       | '*' | '%' | '/' | '//'
  public static boolean Binop(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Binop")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, BINOP, "<binop>");
    r = consumeToken(b, OR);
    if (!r) r = consumeToken(b, AND);
    if (!r) r = consumeToken(b, IS_EQUAL);
    if (!r) r = consumeToken(b, IS_NOT_EQUAL);
    if (!r) r = consumeToken(b, LESS_THAN);
    if (!r) r = consumeToken(b, GREATER_THAN);
    if (!r) r = consumeToken(b, LESS_OR_EQUAL);
    if (!r) r = consumeToken(b, GREATER_OR_EQUAL);
    if (!r) r = consumeToken(b, IN);
    if (!r) r = parseTokens(b, 0, NOT, IN);
    if (!r) r = consumeToken(b, BIT_OR);
    if (!r) r = consumeToken(b, BIT_XOR);
    if (!r) r = consumeToken(b, BIT_AND);
    if (!r) r = consumeToken(b, BIT_SHIFT_LEFT);
    if (!r) r = consumeToken(b, BIT_SHIFT_RIGHT);
    if (!r) r = consumeToken(b, MINUS);
    if (!r) r = consumeToken(b, PLUS);
    if (!r) r = consumeToken(b, ASTERISK);
    if (!r) r = consumeToken(b, MODULO);
    if (!r) r = consumeToken(b, DIV);
    if (!r) r = consumeToken(b, DIV_INT);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // break
  public static boolean BreakStmt(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "BreakStmt")) return false;
    if (!nextTokenIs(b, BREAK)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, BREAK);
    exit_section_(b, m, BREAK_STMT, r);
    return r;
  }

  /* ********************************************************** */
  // '(' (Arguments ','?)? ')'
  public static boolean CallSuffix(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CallSuffix")) return false;
    if (!nextTokenIs(b, LEFT_PAREN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LEFT_PAREN);
    r = r && CallSuffix_1(b, l + 1);
    r = r && consumeToken(b, RIGHT_PAREN);
    exit_section_(b, m, CALL_SUFFIX, r);
    return r;
  }

  // (Arguments ','?)?
  private static boolean CallSuffix_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CallSuffix_1")) return false;
    CallSuffix_1_0(b, l + 1);
    return true;
  }

  // Arguments ','?
  private static boolean CallSuffix_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CallSuffix_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Arguments(b, l + 1);
    r = r && CallSuffix_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ','?
  private static boolean CallSuffix_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CallSuffix_1_0_1")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // for LoopVariables in Test
  //              | if Test
  public static boolean CompClause(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CompClause")) return false;
    if (!nextTokenIs(b, "<comp clause>", FOR, IF)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, COMP_CLAUSE, "<comp clause>");
    r = CompClause_0(b, l + 1);
    if (!r) r = CompClause_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // for LoopVariables in Test
  private static boolean CompClause_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CompClause_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, FOR);
    r = r && LoopVariables(b, l + 1);
    r = r && consumeToken(b, IN);
    r = r && Test(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // if Test
  private static boolean CompClause_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CompClause_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IF);
    r = r && Test(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // continue
  public static boolean ContinueStmt(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ContinueStmt")) return false;
    if (!nextTokenIs(b, CONTINUE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CONTINUE);
    exit_section_(b, m, CONTINUE_STMT, r);
    return r;
  }

  /* ********************************************************** */
  // def IDENTIFIER '(' (Parameters ','?)? ')' ':' Suite
  public static boolean DefStmt(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DefStmt")) return false;
    if (!nextTokenIs(b, DEF)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, DEF, IDENTIFIER, LEFT_PAREN);
    r = r && DefStmt_3(b, l + 1);
    r = r && consumeTokens(b, 0, RIGHT_PAREN, COLON);
    r = r && Suite(b, l + 1);
    exit_section_(b, m, DEF_STMT, r);
    return r;
  }

  // (Parameters ','?)?
  private static boolean DefStmt_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DefStmt_3")) return false;
    DefStmt_3_0(b, l + 1);
    return true;
  }

  // Parameters ','?
  private static boolean DefStmt_3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DefStmt_3_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Parameters(b, l + 1);
    r = r && DefStmt_3_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ','?
  private static boolean DefStmt_3_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DefStmt_3_0_1")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // '{' Entry CompClause* '}'
  public static boolean DictComp(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DictComp")) return false;
    if (!nextTokenIs(b, LEFT_CURLY)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LEFT_CURLY);
    r = r && Entry(b, l + 1);
    r = r && DictComp_2(b, l + 1);
    r = r && consumeToken(b, RIGHT_CURLY);
    exit_section_(b, m, DICT_COMP, r);
    return r;
  }

  // CompClause*
  private static boolean DictComp_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DictComp_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!CompClause(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "DictComp_2", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // '{' (Entries ','?)? '}'
  public static boolean DictExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DictExpr")) return false;
    if (!nextTokenIs(b, LEFT_CURLY)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LEFT_CURLY);
    r = r && DictExpr_1(b, l + 1);
    r = r && consumeToken(b, RIGHT_CURLY);
    exit_section_(b, m, DICT_EXPR, r);
    return r;
  }

  // (Entries ','?)?
  private static boolean DictExpr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DictExpr_1")) return false;
    DictExpr_1_0(b, l + 1);
    return true;
  }

  // Entries ','?
  private static boolean DictExpr_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DictExpr_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Entries(b, l + 1);
    r = r && DictExpr_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ','?
  private static boolean DictExpr_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DictExpr_1_0_1")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // '.' IDENTIFIER
  public static boolean DotSuffix(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DotSuffix")) return false;
    if (!nextTokenIs(b, DOT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, DOT, IDENTIFIER);
    exit_section_(b, m, DOT_SUFFIX, r);
    return r;
  }

  /* ********************************************************** */
  // Entry (',' Entry)*
  public static boolean Entries(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Entries")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ENTRIES, "<entries>");
    r = Entry(b, l + 1);
    r = r && Entries_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (',' Entry)*
  private static boolean Entries_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Entries_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!Entries_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "Entries_1", c)) break;
    }
    return true;
  }

  // ',' Entry
  private static boolean Entries_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Entries_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && Entry(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // Test ':' Test
  public static boolean Entry(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Entry")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ENTRY, "<entry>");
    r = Test(b, l + 1);
    r = r && consumeToken(b, COLON);
    r = r && Test(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // Expression
  public static boolean ExprStmt(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ExprStmt")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, EXPR_STMT, "<expr stmt>");
    r = Expression(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // Test (',' Test)*
  public static boolean Expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Expression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, EXPRESSION, "<expression>");
    r = Test(b, l + 1);
    r = r && Expression_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (',' Test)*
  private static boolean Expression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Expression_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!Expression_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "Expression_1", c)) break;
    }
    return true;
  }

  // ',' Test
  private static boolean Expression_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Expression_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && Test(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // Statement*
  static boolean File(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "File")) return false;
    while (true) {
      int c = current_position_(b);
      if (!Statement(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "File", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // for LoopVariables in Expression ':' Suite
  public static boolean ForStmt(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ForStmt")) return false;
    if (!nextTokenIs(b, FOR)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, FOR);
    r = r && LoopVariables(b, l + 1);
    r = r && consumeToken(b, IN);
    r = r && Expression(b, l + 1);
    r = r && consumeToken(b, COLON);
    r = r && Suite(b, l + 1);
    exit_section_(b, m, FOR_STMT, r);
    return r;
  }

  /* ********************************************************** */
  // BinaryExpr2 if Test else Test IfExpr1?
  public static boolean IfExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfExpr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, IF_EXPR, "<if expr>");
    r = BinaryExpr2(b, l + 1);
    r = r && consumeToken(b, IF);
    r = r && Test(b, l + 1);
    r = r && consumeToken(b, ELSE);
    r = r && Test(b, l + 1);
    r = r && IfExpr_5(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // IfExpr1?
  private static boolean IfExpr_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfExpr_5")) return false;
    IfExpr1(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // IfExpr2 if Test else Test IfExpr1?
  public static boolean IfExpr1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfExpr1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, IF_EXPR_1, "<if expr 1>");
    r = IfExpr2(b, l + 1);
    r = r && consumeToken(b, IF);
    r = r && Test(b, l + 1);
    r = r && consumeToken(b, ELSE);
    r = r && Test(b, l + 1);
    r = r && IfExpr1_5(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // IfExpr1?
  private static boolean IfExpr1_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfExpr1_5")) return false;
    IfExpr1(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // PrimaryExpr if Test else Test
  //           | UnaryExpr if Test else Test
  //           | LambdaExpr if Test else Test
  //           | BinaryExpr if Test else Test
  public static boolean IfExpr2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfExpr2")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, IF_EXPR_2, "<if expr 2>");
    r = IfExpr2_0(b, l + 1);
    if (!r) r = IfExpr2_1(b, l + 1);
    if (!r) r = IfExpr2_2(b, l + 1);
    if (!r) r = IfExpr2_3(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // PrimaryExpr if Test else Test
  private static boolean IfExpr2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfExpr2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = PrimaryExpr(b, l + 1);
    r = r && consumeToken(b, IF);
    r = r && Test(b, l + 1);
    r = r && consumeToken(b, ELSE);
    r = r && Test(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // UnaryExpr if Test else Test
  private static boolean IfExpr2_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfExpr2_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = UnaryExpr(b, l + 1);
    r = r && consumeToken(b, IF);
    r = r && Test(b, l + 1);
    r = r && consumeToken(b, ELSE);
    r = r && Test(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // LambdaExpr if Test else Test
  private static boolean IfExpr2_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfExpr2_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = LambdaExpr(b, l + 1);
    r = r && consumeToken(b, IF);
    r = r && Test(b, l + 1);
    r = r && consumeToken(b, ELSE);
    r = r && Test(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // BinaryExpr if Test else Test
  private static boolean IfExpr2_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfExpr2_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = BinaryExpr(b, l + 1);
    r = r && consumeToken(b, IF);
    r = r && Test(b, l + 1);
    r = r && consumeToken(b, ELSE);
    r = r && Test(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // if Test ':' Suite (elif Test ':' Suite)* (else ':' Suite)?
  public static boolean IfStmt(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfStmt")) return false;
    if (!nextTokenIs(b, IF)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IF);
    r = r && Test(b, l + 1);
    r = r && consumeToken(b, COLON);
    r = r && Suite(b, l + 1);
    r = r && IfStmt_4(b, l + 1);
    r = r && IfStmt_5(b, l + 1);
    exit_section_(b, m, IF_STMT, r);
    return r;
  }

  // (elif Test ':' Suite)*
  private static boolean IfStmt_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfStmt_4")) return false;
    while (true) {
      int c = current_position_(b);
      if (!IfStmt_4_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "IfStmt_4", c)) break;
    }
    return true;
  }

  // elif Test ':' Suite
  private static boolean IfStmt_4_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfStmt_4_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ELIF);
    r = r && Test(b, l + 1);
    r = r && consumeToken(b, COLON);
    r = r && Suite(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (else ':' Suite)?
  private static boolean IfStmt_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfStmt_5")) return false;
    IfStmt_5_0(b, l + 1);
    return true;
  }

  // else ':' Suite
  private static boolean IfStmt_5_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfStmt_5_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, ELSE, COLON);
    r = r && Suite(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // lambda Parameters? ':' Test
  public static boolean LambdaExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LambdaExpr")) return false;
    if (!nextTokenIs(b, LAMBDA)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LAMBDA);
    r = r && LambdaExpr_1(b, l + 1);
    r = r && consumeToken(b, COLON);
    r = r && Test(b, l + 1);
    exit_section_(b, m, LAMBDA_EXPR, r);
    return r;
  }

  // Parameters?
  private static boolean LambdaExpr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LambdaExpr_1")) return false;
    Parameters(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // '[' Test CompClause* ']'
  public static boolean ListComp(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListComp")) return false;
    if (!nextTokenIs(b, LEFT_BRACKET)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LEFT_BRACKET);
    r = r && Test(b, l + 1);
    r = r && ListComp_2(b, l + 1);
    r = r && consumeToken(b, RIGHT_BRACKET);
    exit_section_(b, m, LIST_COMP, r);
    return r;
  }

  // CompClause*
  private static boolean ListComp_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListComp_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!CompClause(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "ListComp_2", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // '[' (Expression ','?)? ']'
  public static boolean ListExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListExpr")) return false;
    if (!nextTokenIs(b, LEFT_BRACKET)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LEFT_BRACKET);
    r = r && ListExpr_1(b, l + 1);
    r = r && consumeToken(b, RIGHT_BRACKET);
    exit_section_(b, m, LIST_EXPR, r);
    return r;
  }

  // (Expression ','?)?
  private static boolean ListExpr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListExpr_1")) return false;
    ListExpr_1_0(b, l + 1);
    return true;
  }

  // Expression ','?
  private static boolean ListExpr_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListExpr_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Expression(b, l + 1);
    r = r && ListExpr_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ','?
  private static boolean ListExpr_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListExpr_1_0_1")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // load '(' STRING (',' (IDENTIFIER '=')? STRING)* ','? ')'
  public static boolean LoadStmt(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LoadStmt")) return false;
    if (!nextTokenIs(b, LOAD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, LOAD, LEFT_PAREN, STRING);
    r = r && LoadStmt_3(b, l + 1);
    r = r && LoadStmt_4(b, l + 1);
    r = r && consumeToken(b, RIGHT_PAREN);
    exit_section_(b, m, LOAD_STMT, r);
    return r;
  }

  // (',' (IDENTIFIER '=')? STRING)*
  private static boolean LoadStmt_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LoadStmt_3")) return false;
    while (true) {
      int c = current_position_(b);
      if (!LoadStmt_3_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "LoadStmt_3", c)) break;
    }
    return true;
  }

  // ',' (IDENTIFIER '=')? STRING
  private static boolean LoadStmt_3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LoadStmt_3_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && LoadStmt_3_0_1(b, l + 1);
    r = r && consumeToken(b, STRING);
    exit_section_(b, m, null, r);
    return r;
  }

  // (IDENTIFIER '=')?
  private static boolean LoadStmt_3_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LoadStmt_3_0_1")) return false;
    LoadStmt_3_0_1_0(b, l + 1);
    return true;
  }

  // IDENTIFIER '='
  private static boolean LoadStmt_3_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LoadStmt_3_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, IDENTIFIER, EQ);
    exit_section_(b, m, null, r);
    return r;
  }

  // ','?
  private static boolean LoadStmt_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LoadStmt_4")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // PrimaryExpr (',' PrimaryExpr)*
  public static boolean LoopVariables(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LoopVariables")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, LOOP_VARIABLES, "<loop variables>");
    r = PrimaryExpr(b, l + 1);
    r = r && LoopVariables_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (',' PrimaryExpr)*
  private static boolean LoopVariables_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LoopVariables_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!LoopVariables_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "LoopVariables_1", c)) break;
    }
    return true;
  }

  // ',' PrimaryExpr
  private static boolean LoopVariables_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LoopVariables_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && PrimaryExpr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // IDENTIFIER
  //           | INT | FLOAT | STRING | BYTES
  //           | ListExpr | ListComp
  //           | DictExpr | DictComp
  //           | '(' (Expression ','?)? ')'
  public static boolean Operand(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Operand")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, OPERAND, "<operand>");
    r = consumeToken(b, IDENTIFIER);
    if (!r) r = consumeToken(b, INT);
    if (!r) r = consumeToken(b, FLOAT);
    if (!r) r = consumeToken(b, STRING);
    if (!r) r = consumeToken(b, BYTES);
    if (!r) r = ListExpr(b, l + 1);
    if (!r) r = ListComp(b, l + 1);
    if (!r) r = DictExpr(b, l + 1);
    if (!r) r = DictComp(b, l + 1);
    if (!r) r = Operand_9(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // '(' (Expression ','?)? ')'
  private static boolean Operand_9(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Operand_9")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LEFT_PAREN);
    r = r && Operand_9_1(b, l + 1);
    r = r && consumeToken(b, RIGHT_PAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  // (Expression ','?)?
  private static boolean Operand_9_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Operand_9_1")) return false;
    Operand_9_1_0(b, l + 1);
    return true;
  }

  // Expression ','?
  private static boolean Operand_9_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Operand_9_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Expression(b, l + 1);
    r = r && Operand_9_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ','?
  private static boolean Operand_9_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Operand_9_1_0_1")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // IDENTIFIER
  //             | IDENTIFIER '=' Test
  //             | '*' IDENTIFIER
  //             | '**' IDENTIFIER
  public static boolean Parameter(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Parameter")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PARAMETER, "<parameter>");
    r = consumeToken(b, IDENTIFIER);
    if (!r) r = Parameter_1(b, l + 1);
    if (!r) r = parseTokens(b, 0, ASTERISK, IDENTIFIER);
    if (!r) r = parseTokens(b, 0, DOUBLE_ASTERISK, IDENTIFIER);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // IDENTIFIER '=' Test
  private static boolean Parameter_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Parameter_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, IDENTIFIER, EQ);
    r = r && Test(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // Parameter (',' Parameter)*
  public static boolean Parameters(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Parameters")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PARAMETERS, "<parameters>");
    r = Parameter(b, l + 1);
    r = r && Parameters_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (',' Parameter)*
  private static boolean Parameters_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Parameters_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!Parameters_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "Parameters_1", c)) break;
    }
    return true;
  }

  // ',' Parameter
  private static boolean Parameters_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Parameters_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && Parameter(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // pass
  public static boolean PassStmt(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PassStmt")) return false;
    if (!nextTokenIs(b, PASS)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PASS);
    exit_section_(b, m, PASS_STMT, r);
    return r;
  }

  /* ********************************************************** */
  // Operand PrimaryExpr1*
  public static boolean PrimaryExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PrimaryExpr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PRIMARY_EXPR, "<primary expr>");
    r = Operand(b, l + 1);
    r = r && PrimaryExpr_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // PrimaryExpr1*
  private static boolean PrimaryExpr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PrimaryExpr_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!PrimaryExpr1(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "PrimaryExpr_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // DotSuffix
  //                | CallSuffix
  //                | SliceSuffix
  public static boolean PrimaryExpr1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PrimaryExpr1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PRIMARY_EXPR_1, "<primary expr 1>");
    r = DotSuffix(b, l + 1);
    if (!r) r = CallSuffix(b, l + 1);
    if (!r) r = SliceSuffix(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // return (<<newLine>> | <<eof>> | Expression)? (<<newLine>> | <<eof>>)
  public static boolean ReturnStmt(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ReturnStmt")) return false;
    if (!nextTokenIs(b, RETURN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, RETURN);
    r = r && ReturnStmt_1(b, l + 1);
    r = r && ReturnStmt_2(b, l + 1);
    exit_section_(b, m, RETURN_STMT, r);
    return r;
  }

  // (<<newLine>> | <<eof>> | Expression)?
  private static boolean ReturnStmt_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ReturnStmt_1")) return false;
    ReturnStmt_1_0(b, l + 1);
    return true;
  }

  // <<newLine>> | <<eof>> | Expression
  private static boolean ReturnStmt_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ReturnStmt_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = newLine(b, l + 1);
    if (!r) r = eof(b, l + 1);
    if (!r) r = Expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // <<newLine>> | <<eof>>
  private static boolean ReturnStmt_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ReturnStmt_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = newLine(b, l + 1);
    if (!r) r = eof(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // SmallStmt (';' SmallStmt)* ';'? (<<newLine>> | <<eof>>)
  public static boolean SimpleStmt(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SimpleStmt")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, SIMPLE_STMT, "<simple stmt>");
    r = SmallStmt(b, l + 1);
    r = r && SimpleStmt_1(b, l + 1);
    r = r && SimpleStmt_2(b, l + 1);
    r = r && SimpleStmt_3(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (';' SmallStmt)*
  private static boolean SimpleStmt_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SimpleStmt_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!SimpleStmt_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "SimpleStmt_1", c)) break;
    }
    return true;
  }

  // ';' SmallStmt
  private static boolean SimpleStmt_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SimpleStmt_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, SEMICOLON);
    r = r && SmallStmt(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ';'?
  private static boolean SimpleStmt_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SimpleStmt_2")) return false;
    consumeToken(b, SEMICOLON);
    return true;
  }

  // <<newLine>> | <<eof>>
  private static boolean SimpleStmt_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SimpleStmt_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = newLine(b, l + 1);
    if (!r) r = eof(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '[' Expression? ':' Test? (':' Test?)? ']'
  //               | '[' Expression ']'
  public static boolean SliceSuffix(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SliceSuffix")) return false;
    if (!nextTokenIs(b, LEFT_BRACKET)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = SliceSuffix_0(b, l + 1);
    if (!r) r = SliceSuffix_1(b, l + 1);
    exit_section_(b, m, SLICE_SUFFIX, r);
    return r;
  }

  // '[' Expression? ':' Test? (':' Test?)? ']'
  private static boolean SliceSuffix_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SliceSuffix_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LEFT_BRACKET);
    r = r && SliceSuffix_0_1(b, l + 1);
    r = r && consumeToken(b, COLON);
    r = r && SliceSuffix_0_3(b, l + 1);
    r = r && SliceSuffix_0_4(b, l + 1);
    r = r && consumeToken(b, RIGHT_BRACKET);
    exit_section_(b, m, null, r);
    return r;
  }

  // Expression?
  private static boolean SliceSuffix_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SliceSuffix_0_1")) return false;
    Expression(b, l + 1);
    return true;
  }

  // Test?
  private static boolean SliceSuffix_0_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SliceSuffix_0_3")) return false;
    Test(b, l + 1);
    return true;
  }

  // (':' Test?)?
  private static boolean SliceSuffix_0_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SliceSuffix_0_4")) return false;
    SliceSuffix_0_4_0(b, l + 1);
    return true;
  }

  // ':' Test?
  private static boolean SliceSuffix_0_4_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SliceSuffix_0_4_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COLON);
    r = r && SliceSuffix_0_4_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // Test?
  private static boolean SliceSuffix_0_4_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SliceSuffix_0_4_0_1")) return false;
    Test(b, l + 1);
    return true;
  }

  // '[' Expression ']'
  private static boolean SliceSuffix_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SliceSuffix_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LEFT_BRACKET);
    r = r && Expression(b, l + 1);
    r = r && consumeToken(b, RIGHT_BRACKET);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // ReturnStmt
  //             | BreakStmt | ContinueStmt | PassStmt
  //             | AssignStmt
  //             | ExprStmt
  //             | LoadStmt
  public static boolean SmallStmt(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SmallStmt")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, SMALL_STMT, "<small stmt>");
    r = ReturnStmt(b, l + 1);
    if (!r) r = BreakStmt(b, l + 1);
    if (!r) r = ContinueStmt(b, l + 1);
    if (!r) r = PassStmt(b, l + 1);
    if (!r) r = AssignStmt(b, l + 1);
    if (!r) r = ExprStmt(b, l + 1);
    if (!r) r = LoadStmt(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // DefStmt
  //             | IfStmt
  //             | ForStmt
  //             | SimpleStmt
  public static boolean Statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Statement")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, STATEMENT, "<statement>");
    r = DefStmt(b, l + 1);
    if (!r) r = IfStmt(b, l + 1);
    if (!r) r = ForStmt(b, l + 1);
    if (!r) r = SimpleStmt(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // <<checkIfIndentIncreasesAndSaveIt>> (<<checkIndent>> Statement)+ <<finishBlock>>
  //         | <<noNewLineOrLineBreak>> SimpleStmt
  public static boolean Suite(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Suite")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, SUITE, "<suite>");
    r = Suite_0(b, l + 1);
    if (!r) r = Suite_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // <<checkIfIndentIncreasesAndSaveIt>> (<<checkIndent>> Statement)+ <<finishBlock>>
  private static boolean Suite_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Suite_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = checkIfIndentIncreasesAndSaveIt(b, l + 1);
    r = r && Suite_0_1(b, l + 1);
    r = r && finishBlock(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (<<checkIndent>> Statement)+
  private static boolean Suite_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Suite_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Suite_0_1_0(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!Suite_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "Suite_0_1", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // <<checkIndent>> Statement
  private static boolean Suite_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Suite_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = checkIndent(b, l + 1);
    r = r && Statement(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // <<noNewLineOrLineBreak>> SimpleStmt
  private static boolean Suite_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Suite_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = noNewLineOrLineBreak(b, l + 1);
    r = r && SimpleStmt(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // IfExpr
  //         | BinaryExpr
  //        | UnaryExpr
  //        | PrimaryExpr
  //        | LambdaExpr
  public static boolean Test(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Test")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, TEST, "<test>");
    r = IfExpr(b, l + 1);
    if (!r) r = BinaryExpr(b, l + 1);
    if (!r) r = UnaryExpr(b, l + 1);
    if (!r) r = PrimaryExpr(b, l + 1);
    if (!r) r = LambdaExpr(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '+' Test
  //             | '-' Test
  //             | '~' Test
  //             | not Test
  public static boolean UnaryExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "UnaryExpr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, UNARY_EXPR, "<unary expr>");
    r = UnaryExpr_0(b, l + 1);
    if (!r) r = UnaryExpr_1(b, l + 1);
    if (!r) r = UnaryExpr_2(b, l + 1);
    if (!r) r = UnaryExpr_3(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // '+' Test
  private static boolean UnaryExpr_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "UnaryExpr_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PLUS);
    r = r && Test(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // '-' Test
  private static boolean UnaryExpr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "UnaryExpr_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, MINUS);
    r = r && Test(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // '~' Test
  private static boolean UnaryExpr_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "UnaryExpr_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, TYLDA);
    r = r && Test(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // not Test
  private static boolean UnaryExpr_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "UnaryExpr_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, NOT);
    r = r && Test(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

}
