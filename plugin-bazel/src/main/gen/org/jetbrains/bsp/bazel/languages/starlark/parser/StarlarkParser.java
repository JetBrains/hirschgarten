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
    b = adapt_builder_(t, b, this, EXTENDS_SETS_);
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

  public static final TokenSet[] EXTENDS_SETS_ = new TokenSet[] {
    create_token_set_(DEF_STATEMENT, FOR_STATEMENT, IF_STATEMENT, SIMPLE_STATEMENT,
      STATEMENT),
    create_token_set_(BINARY_EXPRESSION, EXPRESSION, IF_EXPRESSION, LAMBDA_EXPRESSION,
      PRIMARY_EXPRESSION, UNARY_EXPRESSION),
    create_token_set_(ASSIGN_STMT, BREAK_STMT, CONTINUE_STMT, EXPR_STMT,
      LOAD_STMT, PASS_STMT, RETURN_STMT, STMT),
  };

  /* ********************************************************** */
  // IDENTIFIER '=' Expression | '*' Expression | '**' Expression | Expression
  public static boolean Argument(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Argument")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ARGUMENT, "<argument>");
    r = Argument_0(b, l + 1);
    if (!r) r = Argument_1(b, l + 1);
    if (!r) r = Argument_2(b, l + 1);
    if (!r) r = Expression(b, l + 1, -1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // IDENTIFIER '=' Expression
  private static boolean Argument_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Argument_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, IDENTIFIER, EQ);
    r = r && Expression(b, l + 1, -1);
    exit_section_(b, m, null, r);
    return r;
  }

  // '*' Expression
  private static boolean Argument_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Argument_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ASTERISK);
    r = r && Expression(b, l + 1, -1);
    exit_section_(b, m, null, r);
    return r;
  }

  // '**' Expression
  private static boolean Argument_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Argument_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, DOUBLE_ASTERISK);
    r = r && Expression(b, l + 1, -1);
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
  // '=' | '+=' | '-=' | '*=' | '/=' | '//=' | '%=' | '&=' | '|=' | '^=' | '<<=' | '>>='
  public static boolean AssignOperator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "AssignOperator")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ASSIGN_OPERATOR, "<assign operator>");
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
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // ExprStmt NNL AssignOperator NNL ExprStmt
  public static boolean AssignStmt(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "AssignStmt")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ASSIGN_STMT, "<assign stmt>");
    r = ExprStmt(b, l + 1);
    r = r && NNL(b, l + 1);
    r = r && AssignOperator(b, l + 1);
    r = r && NNL(b, l + 1);
    r = r && ExprStmt(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '==' | '!=' | '<' | '>' | '<=' | '>='
  //                  | '*' | '%' | '/' | '//' | '+' | '-'
  //                  | '|' | '^' | '&' | '<<' | '>>'
  //                  | in | not in
  //                  | or | and
  public static boolean BinaryOperator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "BinaryOperator")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, BINARY_OPERATOR, "<binary operator>");
    r = consumeToken(b, IS_EQUAL);
    if (!r) r = consumeToken(b, IS_NOT_EQUAL);
    if (!r) r = consumeToken(b, LESS_THAN);
    if (!r) r = consumeToken(b, GREATER_THAN);
    if (!r) r = consumeToken(b, LESS_OR_EQUAL);
    if (!r) r = consumeToken(b, GREATER_OR_EQUAL);
    if (!r) r = consumeToken(b, ASTERISK);
    if (!r) r = consumeToken(b, MODULO);
    if (!r) r = consumeToken(b, DIV);
    if (!r) r = consumeToken(b, DIV_INT);
    if (!r) r = consumeToken(b, PLUS);
    if (!r) r = consumeToken(b, MINUS);
    if (!r) r = consumeToken(b, BIT_OR);
    if (!r) r = consumeToken(b, BIT_XOR);
    if (!r) r = consumeToken(b, BIT_AND);
    if (!r) r = consumeToken(b, BIT_SHIFT_LEFT);
    if (!r) r = consumeToken(b, BIT_SHIFT_RIGHT);
    if (!r) r = consumeToken(b, IN);
    if (!r) r = parseTokens(b, 0, NOT, IN);
    if (!r) r = consumeToken(b, OR);
    if (!r) r = consumeToken(b, AND);
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
  // '(' [Arguments [',']] ')'
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

  // [Arguments [',']]
  private static boolean CallSuffix_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CallSuffix_1")) return false;
    CallSuffix_1_0(b, l + 1);
    return true;
  }

  // Arguments [',']
  private static boolean CallSuffix_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CallSuffix_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Arguments(b, l + 1);
    r = r && CallSuffix_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // [',']
  private static boolean CallSuffix_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CallSuffix_1_0_1")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // (for NNL LoopVariables NNL in | if) NNL IfLastExpr
  public static boolean CompClause(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CompClause")) return false;
    if (!nextTokenIs(b, "<comp clause>", FOR, IF)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, COMP_CLAUSE, "<comp clause>");
    r = CompClause_0(b, l + 1);
    r = r && NNL(b, l + 1);
    r = r && IfLastExpr(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // for NNL LoopVariables NNL in | if
  private static boolean CompClause_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CompClause_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = CompClause_0_0(b, l + 1);
    if (!r) r = consumeToken(b, IF);
    exit_section_(b, m, null, r);
    return r;
  }

  // for NNL LoopVariables NNL in
  private static boolean CompClause_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "CompClause_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, FOR);
    r = r && NNL(b, l + 1);
    r = r && LoopVariables(b, l + 1);
    r = r && NNL(b, l + 1);
    r = r && consumeToken(b, IN);
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
  // def NNL IDENTIFIER NNL '(' [Parameters [',']] ')' NNL ':' Suite
  public static boolean DefStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DefStatement")) return false;
    if (!nextTokenIs(b, DEF)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, DEF);
    r = r && NNL(b, l + 1);
    r = r && consumeToken(b, IDENTIFIER);
    r = r && NNL(b, l + 1);
    r = r && consumeToken(b, LEFT_PAREN);
    r = r && DefStatement_5(b, l + 1);
    r = r && consumeToken(b, RIGHT_PAREN);
    r = r && NNL(b, l + 1);
    r = r && consumeToken(b, COLON);
    r = r && Suite(b, l + 1);
    exit_section_(b, m, DEF_STATEMENT, r);
    return r;
  }

  // [Parameters [',']]
  private static boolean DefStatement_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DefStatement_5")) return false;
    DefStatement_5_0(b, l + 1);
    return true;
  }

  // Parameters [',']
  private static boolean DefStatement_5_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DefStatement_5_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Parameters(b, l + 1);
    r = r && DefStatement_5_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // [',']
  private static boolean DefStatement_5_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DefStatement_5_0_1")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // '{' IfLastEntry CompClause* '}'
  public static boolean DictComp(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DictComp")) return false;
    if (!nextTokenIs(b, LEFT_CURLY)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LEFT_CURLY);
    r = r && IfLastEntry(b, l + 1);
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
  // '{' [Entries [',']] '}'
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

  // [Entries [',']]
  private static boolean DictExpr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DictExpr_1")) return false;
    DictExpr_1_0(b, l + 1);
    return true;
  }

  // Entries [',']
  private static boolean DictExpr_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DictExpr_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Entries(b, l + 1);
    r = r && DictExpr_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // [',']
  private static boolean DictExpr_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DictExpr_1_0_1")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // '.' NNL IDENTIFIER
  public static boolean DotSuffix(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "DotSuffix")) return false;
    if (!nextTokenIs(b, DOT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, DOT);
    r = r && NNL(b, l + 1);
    r = r && consumeToken(b, IDENTIFIER);
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
  // Expression NNL ':' NNL Expression
  public static boolean Entry(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Entry")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ENTRY, "<entry>");
    r = Expression(b, l + 1, -1);
    r = r && NNL(b, l + 1);
    r = r && consumeToken(b, COLON);
    r = r && NNL(b, l + 1);
    r = r && Expression(b, l + 1, -1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // Expression (NNL ',' Expression)*
  public static boolean ExprStmt(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ExprStmt")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, EXPR_STMT, "<expr stmt>");
    r = Expression(b, l + 1, -1);
    r = r && ExprStmt_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (NNL ',' Expression)*
  private static boolean ExprStmt_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ExprStmt_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!ExprStmt_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "ExprStmt_1", c)) break;
    }
    return true;
  }

  // NNL ',' Expression
  private static boolean ExprStmt_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ExprStmt_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = NNL(b, l + 1);
    r = r && consumeToken(b, COMMA);
    r = r && Expression(b, l + 1, -1);
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
  // for NNL LoopVariables NNL in NNL ExprStmt NNL ':' Suite
  public static boolean ForStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ForStatement")) return false;
    if (!nextTokenIs(b, FOR)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, FOR);
    r = r && NNL(b, l + 1);
    r = r && LoopVariables(b, l + 1);
    r = r && NNL(b, l + 1);
    r = r && consumeToken(b, IN);
    r = r && NNL(b, l + 1);
    r = r && ExprStmt(b, l + 1);
    r = r && NNL(b, l + 1);
    r = r && consumeToken(b, COLON);
    r = r && Suite(b, l + 1);
    exit_section_(b, m, FOR_STATEMENT, r);
    return r;
  }

  /* ********************************************************** */
  // Expression NNL ':' NNL IfLastExpr
  public static boolean IfLastEntry(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfLastEntry")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, IF_LAST_ENTRY, "<if last entry>");
    r = Expression(b, l + 1, -1);
    r = r && NNL(b, l + 1);
    r = r && consumeToken(b, COLON);
    r = r && NNL(b, l + 1);
    r = r && IfLastExpr(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // BinaryExpression | UnaryExpression | PrimaryExpression | LambdaExpression | IfExpression
  public static boolean IfLastExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfLastExpr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, IF_LAST_EXPR, "<if last expr>");
    r = Expression(b, l + 1, 0);
    if (!r) r = UnaryExpression(b, l + 1);
    if (!r) r = PrimaryExpression(b, l + 1);
    if (!r) r = LambdaExpression(b, l + 1);
    if (!r) r = Expression(b, l + 1, -1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // if NNL Expression NNL ':' Suite (elif NNL Expression  NNL ':' Suite)* [else NNL ':' Suite]
  public static boolean IfStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfStatement")) return false;
    if (!nextTokenIs(b, IF)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IF);
    r = r && NNL(b, l + 1);
    r = r && Expression(b, l + 1, -1);
    r = r && NNL(b, l + 1);
    r = r && consumeToken(b, COLON);
    r = r && Suite(b, l + 1);
    r = r && IfStatement_6(b, l + 1);
    r = r && IfStatement_7(b, l + 1);
    exit_section_(b, m, IF_STATEMENT, r);
    return r;
  }

  // (elif NNL Expression  NNL ':' Suite)*
  private static boolean IfStatement_6(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfStatement_6")) return false;
    while (true) {
      int c = current_position_(b);
      if (!IfStatement_6_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "IfStatement_6", c)) break;
    }
    return true;
  }

  // elif NNL Expression  NNL ':' Suite
  private static boolean IfStatement_6_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfStatement_6_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ELIF);
    r = r && NNL(b, l + 1);
    r = r && Expression(b, l + 1, -1);
    r = r && NNL(b, l + 1);
    r = r && consumeToken(b, COLON);
    r = r && Suite(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // [else NNL ':' Suite]
  private static boolean IfStatement_7(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfStatement_7")) return false;
    IfStatement_7_0(b, l + 1);
    return true;
  }

  // else NNL ':' Suite
  private static boolean IfStatement_7_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfStatement_7_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ELSE);
    r = r && NNL(b, l + 1);
    r = r && consumeToken(b, COLON);
    r = r && Suite(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '[' IfLastExpr CompClause* ']'
  public static boolean ListComp(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListComp")) return false;
    if (!nextTokenIs(b, LEFT_BRACKET)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LEFT_BRACKET);
    r = r && IfLastExpr(b, l + 1);
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
  // '[' [ExprStmt [',']] ']'
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

  // [ExprStmt [',']]
  private static boolean ListExpr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListExpr_1")) return false;
    ListExpr_1_0(b, l + 1);
    return true;
  }

  // ExprStmt [',']
  private static boolean ListExpr_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListExpr_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = ExprStmt(b, l + 1);
    r = r && ListExpr_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // [',']
  private static boolean ListExpr_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ListExpr_1_0_1")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // load NNL '(' STRING (',' [IDENTIFIER '='] STRING)* [','] ')'
  public static boolean LoadStmt(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LoadStmt")) return false;
    if (!nextTokenIs(b, LOAD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LOAD);
    r = r && NNL(b, l + 1);
    r = r && consumeTokens(b, 0, LEFT_PAREN, STRING);
    r = r && LoadStmt_4(b, l + 1);
    r = r && LoadStmt_5(b, l + 1);
    r = r && consumeToken(b, RIGHT_PAREN);
    exit_section_(b, m, LOAD_STMT, r);
    return r;
  }

  // (',' [IDENTIFIER '='] STRING)*
  private static boolean LoadStmt_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LoadStmt_4")) return false;
    while (true) {
      int c = current_position_(b);
      if (!LoadStmt_4_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "LoadStmt_4", c)) break;
    }
    return true;
  }

  // ',' [IDENTIFIER '='] STRING
  private static boolean LoadStmt_4_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LoadStmt_4_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && LoadStmt_4_0_1(b, l + 1);
    r = r && consumeToken(b, STRING);
    exit_section_(b, m, null, r);
    return r;
  }

  // [IDENTIFIER '=']
  private static boolean LoadStmt_4_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LoadStmt_4_0_1")) return false;
    parseTokens(b, 0, IDENTIFIER, EQ);
    return true;
  }

  // [',']
  private static boolean LoadStmt_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LoadStmt_5")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // PrimaryExpression (NNL ',' PrimaryExpression)*
  public static boolean LoopVariables(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LoopVariables")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, LOOP_VARIABLES, "<loop variables>");
    r = PrimaryExpression(b, l + 1);
    r = r && LoopVariables_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (NNL ',' PrimaryExpression)*
  private static boolean LoopVariables_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LoopVariables_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!LoopVariables_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "LoopVariables_1", c)) break;
    }
    return true;
  }

  // NNL ',' PrimaryExpression
  private static boolean LoopVariables_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LoopVariables_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = NNL(b, l + 1);
    r = r && consumeToken(b, COMMA);
    r = r && PrimaryExpression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // !<<newLine>>
  static boolean NNL(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "NNL")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !newLine(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // IDENTIFIER | INT | FLOAT | STRING | BYTES
  //           | ListComp | ListExpr | DictComp | DictExpr
  //           | '(' [ExprStmt [',']] ')'
  public static boolean Operand(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Operand")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, OPERAND, "<operand>");
    r = consumeToken(b, IDENTIFIER);
    if (!r) r = consumeToken(b, INT);
    if (!r) r = consumeToken(b, FLOAT);
    if (!r) r = consumeToken(b, STRING);
    if (!r) r = consumeToken(b, BYTES);
    if (!r) r = ListComp(b, l + 1);
    if (!r) r = ListExpr(b, l + 1);
    if (!r) r = DictComp(b, l + 1);
    if (!r) r = DictExpr(b, l + 1);
    if (!r) r = Operand_9(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // '(' [ExprStmt [',']] ')'
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

  // [ExprStmt [',']]
  private static boolean Operand_9_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Operand_9_1")) return false;
    Operand_9_1_0(b, l + 1);
    return true;
  }

  // ExprStmt [',']
  private static boolean Operand_9_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Operand_9_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = ExprStmt(b, l + 1);
    r = r && Operand_9_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // [',']
  private static boolean Operand_9_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Operand_9_1_0_1")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // IDENTIFIER '=' Expression | IDENTIFIER | '*' IDENTIFIER | '**' IDENTIFIER
  public static boolean Parameter(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Parameter")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PARAMETER, "<parameter>");
    r = Parameter_0(b, l + 1);
    if (!r) r = consumeToken(b, IDENTIFIER);
    if (!r) r = parseTokens(b, 0, ASTERISK, IDENTIFIER);
    if (!r) r = parseTokens(b, 0, DOUBLE_ASTERISK, IDENTIFIER);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // IDENTIFIER '=' Expression
  private static boolean Parameter_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Parameter_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, IDENTIFIER, EQ);
    r = r && Expression(b, l + 1, -1);
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
  // return [NNL ExprStmt] (<<newLine>> | <<eof>>)
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

  // [NNL ExprStmt]
  private static boolean ReturnStmt_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ReturnStmt_1")) return false;
    ReturnStmt_1_0(b, l + 1);
    return true;
  }

  // NNL ExprStmt
  private static boolean ReturnStmt_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ReturnStmt_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = NNL(b, l + 1);
    r = r && ExprStmt(b, l + 1);
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
  // Stmt (NNL ';' NNL Stmt)* [NNL ';'] (<<newLine>> | <<eof>>)
  public static boolean SimpleStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SimpleStatement")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, SIMPLE_STATEMENT, "<simple statement>");
    r = Stmt(b, l + 1);
    r = r && SimpleStatement_1(b, l + 1);
    r = r && SimpleStatement_2(b, l + 1);
    r = r && SimpleStatement_3(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (NNL ';' NNL Stmt)*
  private static boolean SimpleStatement_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SimpleStatement_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!SimpleStatement_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "SimpleStatement_1", c)) break;
    }
    return true;
  }

  // NNL ';' NNL Stmt
  private static boolean SimpleStatement_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SimpleStatement_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = NNL(b, l + 1);
    r = r && consumeToken(b, SEMICOLON);
    r = r && NNL(b, l + 1);
    r = r && Stmt(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // [NNL ';']
  private static boolean SimpleStatement_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SimpleStatement_2")) return false;
    SimpleStatement_2_0(b, l + 1);
    return true;
  }

  // NNL ';'
  private static boolean SimpleStatement_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SimpleStatement_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = NNL(b, l + 1);
    r = r && consumeToken(b, SEMICOLON);
    exit_section_(b, m, null, r);
    return r;
  }

  // <<newLine>> | <<eof>>
  private static boolean SimpleStatement_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SimpleStatement_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = newLine(b, l + 1);
    if (!r) r = eof(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '[' [ExprStmt] ':' [Expression] [':' [Expression]] ']' | '[' ExprStmt ']'
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

  // '[' [ExprStmt] ':' [Expression] [':' [Expression]] ']'
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

  // [ExprStmt]
  private static boolean SliceSuffix_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SliceSuffix_0_1")) return false;
    ExprStmt(b, l + 1);
    return true;
  }

  // [Expression]
  private static boolean SliceSuffix_0_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SliceSuffix_0_3")) return false;
    Expression(b, l + 1, -1);
    return true;
  }

  // [':' [Expression]]
  private static boolean SliceSuffix_0_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SliceSuffix_0_4")) return false;
    SliceSuffix_0_4_0(b, l + 1);
    return true;
  }

  // ':' [Expression]
  private static boolean SliceSuffix_0_4_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SliceSuffix_0_4_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COLON);
    r = r && SliceSuffix_0_4_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // [Expression]
  private static boolean SliceSuffix_0_4_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SliceSuffix_0_4_0_1")) return false;
    Expression(b, l + 1, -1);
    return true;
  }

  // '[' ExprStmt ']'
  private static boolean SliceSuffix_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "SliceSuffix_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LEFT_BRACKET);
    r = r && ExprStmt(b, l + 1);
    r = r && consumeToken(b, RIGHT_BRACKET);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // DefStatement | IfStatement | ForStatement | SimpleStatement
  public static boolean Statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Statement")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, STATEMENT, "<statement>");
    r = DefStatement(b, l + 1);
    if (!r) r = IfStatement(b, l + 1);
    if (!r) r = ForStatement(b, l + 1);
    if (!r) r = SimpleStatement(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // ReturnStmt | BreakStmt | ContinueStmt | PassStmt | AssignStmt | ExprStmt | LoadStmt
  public static boolean Stmt(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Stmt")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, STMT, "<stmt>");
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
  // NNL (DotSuffix | CallSuffix | SliceSuffix)
  static boolean Suffix(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Suffix")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = NNL(b, l + 1);
    r = r && Suffix_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // DotSuffix | CallSuffix | SliceSuffix
  private static boolean Suffix_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Suffix_1")) return false;
    boolean r;
    r = DotSuffix(b, l + 1);
    if (!r) r = CallSuffix(b, l + 1);
    if (!r) r = SliceSuffix(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // <<checkIfIndentIncreasesAndSaveIt>> (<<checkIndent>> Statement)+ <<finishBlock>>
  //                 | NNL SimpleStatement
  static boolean Suite(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Suite")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Suite_0(b, l + 1);
    if (!r) r = Suite_1(b, l + 1);
    exit_section_(b, m, null, r);
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

  // NNL SimpleStatement
  private static boolean Suite_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Suite_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = NNL(b, l + 1);
    r = r && SimpleStatement(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '+' | '-' | '~' | 'not'
  public static boolean UnaryOperator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "UnaryOperator")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, UNARY_OPERATOR, "<unary operator>");
    r = consumeToken(b, PLUS);
    if (!r) r = consumeToken(b, MINUS);
    if (!r) r = consumeToken(b, TYLDA);
    if (!r) r = consumeToken(b, NOT);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // Expression root: Expression
  // Operator priority table:
  // 0: BINARY(IfExpression)
  // 1: N_ARY(BinaryExpression)
  // 2: PREFIX(UnaryExpression)
  // 3: ATOM(PrimaryExpression)
  // 4: PREFIX(LambdaExpression)
  public static boolean Expression(PsiBuilder b, int l, int g) {
    if (!recursion_guard_(b, l, "Expression")) return false;
    addVariant(b, "<expression>");
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, "<expression>");
    r = UnaryExpression(b, l + 1);
    if (!r) r = PrimaryExpression(b, l + 1);
    if (!r) r = LambdaExpression(b, l + 1);
    p = r;
    r = r && Expression_0(b, l + 1, g);
    exit_section_(b, l, m, null, r, p, null);
    return r || p;
  }

  public static boolean Expression_0(PsiBuilder b, int l, int g) {
    if (!recursion_guard_(b, l, "Expression_0")) return false;
    boolean r = true;
    while (true) {
      Marker m = enter_section_(b, l, _LEFT_, null);
      if (g < 0 && IfExpression_0(b, l + 1)) {
        r = report_error_(b, Expression(b, l, 0));
        r = IfExpression_1(b, l + 1) && r;
        exit_section_(b, l, m, IF_EXPRESSION, r, true, null);
      }
      else if (g < 1 && BinaryExpression_0(b, l + 1)) {
        while (true) {
          r = report_error_(b, Expression(b, l, 1));
          if (!BinaryExpression_0(b, l + 1)) break;
        }
        exit_section_(b, l, m, BINARY_EXPRESSION, r, true, null);
      }
      else {
        exit_section_(b, l, m, null, false, false, null);
        break;
      }
    }
    return r;
  }

  // NNL if NNL
  private static boolean IfExpression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfExpression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = NNL(b, l + 1);
    r = r && consumeToken(b, IF);
    r = r && NNL(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // NNL else NNL Expression
  private static boolean IfExpression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfExpression_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = NNL(b, l + 1);
    r = r && consumeToken(b, ELSE);
    r = r && NNL(b, l + 1);
    r = r && Expression(b, l + 1, -1);
    exit_section_(b, m, null, r);
    return r;
  }

  // NNL BinaryOperator NNL
  private static boolean BinaryExpression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "BinaryExpression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = NNL(b, l + 1);
    r = r && BinaryOperator(b, l + 1);
    r = r && NNL(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  public static boolean UnaryExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "UnaryExpression")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = UnaryExpression_0(b, l + 1);
    p = r;
    r = p && Expression(b, l, 2);
    exit_section_(b, l, m, UNARY_EXPRESSION, r, p, null);
    return r || p;
  }

  // UnaryOperator NNL
  private static boolean UnaryExpression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "UnaryExpression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = UnaryOperator(b, l + 1);
    r = r && NNL(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // Operand Suffix*
  public static boolean PrimaryExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PrimaryExpression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PRIMARY_EXPRESSION, "<primary expression>");
    r = Operand(b, l + 1);
    r = r && PrimaryExpression_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // Suffix*
  private static boolean PrimaryExpression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PrimaryExpression_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!Suffix(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "PrimaryExpression_1", c)) break;
    }
    return true;
  }

  public static boolean LambdaExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LambdaExpression")) return false;
    if (!nextTokenIsSmart(b, LAMBDA)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = LambdaExpression_0(b, l + 1);
    p = r;
    r = p && Expression(b, l, -1);
    exit_section_(b, l, m, LAMBDA_EXPRESSION, r, p, null);
    return r || p;
  }

  // lambda NNL [Parameters] NNL ':' NNL
  private static boolean LambdaExpression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LambdaExpression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, LAMBDA);
    r = r && NNL(b, l + 1);
    r = r && LambdaExpression_0_2(b, l + 1);
    r = r && NNL(b, l + 1);
    r = r && consumeToken(b, COLON);
    r = r && NNL(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // [Parameters]
  private static boolean LambdaExpression_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LambdaExpression_0_2")) return false;
    Parameters(b, l + 1);
    return true;
  }

}
