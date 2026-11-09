// This is a generated file. Not intended for manual editing.
package org.jetbrains.bazel.languages.starlark.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static org.jetbrains.bazel.languages.starlark.parser._StarlarkParserTypes.*;
import static org.jetbrains.bazel.languages.starlark.parser.StarlarkParserUtil.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;
import static org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes.*;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class _StarlarkParser implements PsiParser, LightPsiParser {

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
    return starlark_file(b, l + 1);
  }

  /* ********************************************************** */
  // multiplicative_level additive_tail*
  static boolean additive_level(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "additive_level")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = multiplicative_level(b, l + 1);
    r = r && additive_level_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // additive_tail*
  private static boolean additive_level_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "additive_level_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!additive_tail(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "additive_level_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // ('+' | '-') multiplicative_level
  public static boolean additive_tail(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "additive_tail")) return false;
    if (!nextTokenIsFast(b, MINUS, PLUS)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, BINARY_EXPRESSION, "<additive tail>");
    r = additive_tail_0(b, l + 1);
    p = r; // pin = 1
    r = r && multiplicative_level(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // '+' | '-'
  private static boolean additive_tail_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "additive_tail_0")) return false;
    boolean r;
    r = consumeTokenFast(b, PLUS);
    if (!r) r = consumeTokenFast(b, MINUS);
    return r;
  }

  /* ********************************************************** */
  // comparison_level and_tail*
  static boolean and_level(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "and_level")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = comparison_level(b, l + 1);
    r = r && and_level_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // and_tail*
  private static boolean and_level_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "and_level_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!and_tail(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "and_level_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // 'and' comparison_level
  public static boolean and_tail(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "and_tail")) return false;
    if (!nextTokenIsFast(b, AND_KEYWORD)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, BINARY_EXPRESSION, null);
    r = consumeTokenFast(b, AND_KEYWORD);
    p = r; // pin = 1
    r = r && comparison_level(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // star_argument_expression | named_argument_expression | argument_expression
  static boolean argument(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<argument>");
    r = star_argument_expression(b, l + 1);
    if (!r) r = named_argument_expression(b, l + 1);
    if (!r) r = argument_expression(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // test
  public static boolean argument_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument_expression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ARGUMENT_EXPRESSION, "<argument expression>");
    r = test(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // argument
  static boolean argument_item(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument_item")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<argument>");
    r = argument(b, l + 1);
    exit_section_(b, l, m, r, false, _StarlarkParser::argument_recover);
    return r;
  }

  /* ********************************************************** */
  // '(' <<plainMode arguments>>? ')'
  public static boolean argument_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument_list")) return false;
    if (!nextTokenIs(b, LPAR)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, ARGUMENT_LIST, null);
    r = consumeToken(b, LPAR);
    p = r; // pin = 1
    r = r && report_error_(b, argument_list_1(b, l + 1));
    r = p && consumeToken(b, RPAR) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // <<plainMode arguments>>?
  private static boolean argument_list_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument_list_1")) return false;
    plainMode(b, l + 1, _StarlarkParser::arguments);
    return true;
  }

  /* ********************************************************** */
  // !(',' | close_bracket | header_colon | STATEMENT_BREAK | argument_start)
  static boolean argument_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !argument_recover_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ',' | close_bracket | header_colon | STATEMENT_BREAK | argument_start
  private static boolean argument_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument_recover_0")) return false;
    boolean r;
    r = consumeToken(b, COMMA);
    if (!r) r = close_bracket(b, l + 1);
    if (!r) r = header_colon(b, l + 1);
    if (!r) r = consumeToken(b, STATEMENT_BREAK);
    if (!r) r = argument_start(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // expression_start | '**'
  static boolean argument_start(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument_start")) return false;
    boolean r;
    r = expression_start(b, l + 1);
    if (!r) r = consumeToken(b, EXP);
    return r;
  }

  /* ********************************************************** */
  // !')' argument_item <<comma_tail argument_start ')' argument_item>>* ','?
  static boolean arguments(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arguments")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = arguments_0(b, l + 1);
    p = r; // pin = 1
    r = r && report_error_(b, argument_item(b, l + 1));
    r = p && report_error_(b, arguments_2(b, l + 1)) && r;
    r = p && arguments_3(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // !')'
  private static boolean arguments_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arguments_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !consumeToken(b, RPAR);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // <<comma_tail argument_start ')' argument_item>>*
  private static boolean arguments_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arguments_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!comma_tail(b, l + 1, _StarlarkParser::argument_start, RPAR_parser_, _StarlarkParser::argument_item)) break;
      if (!empty_element_parsed_guard_(b, "arguments_2", c)) break;
    }
    return true;
  }

  // ','?
  private static boolean arguments_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arguments_3")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // <<lookahead expression_start>> <<atAssignment>> <<targetMode expression>> '='
  static boolean assignment_head(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "assignment_head")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = lookahead(b, l + 1, _StarlarkParser::expression_start);
    r = r && atAssignment(b, l + 1);
    p = r; // pin = 2
    r = r && report_error_(b, targetMode(b, l + 1, _StarlarkParser::expression));
    r = p && consumeToken(b, EQ) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // assignment_head+ expression statement_end
  public static boolean assignment_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "assignment_statement")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, ASSIGNMENT_STATEMENT, "<assignment statement>");
    r = assignment_statement_0(b, l + 1);
    p = r; // pin = 1
    r = r && report_error_(b, expression(b, l + 1));
    r = p && statement_end(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // assignment_head+
  private static boolean assignment_statement_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "assignment_statement_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = assignment_head(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!assignment_head(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "assignment_statement_0", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // <<attributeTarget>>
  public static boolean attribute_target(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "attribute_target")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_ | _UPPER_, TARGET_EXPRESSION, "<attribute target>");
    r = attributeTarget(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // <<lookahead expression_start>> <<atAugAssignment>> expression aug_operator expression statement_end
  public static boolean aug_assignment_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "aug_assignment_statement")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, AUG_ASSIGNMENT_STATEMENT, "<aug assignment statement>");
    r = lookahead(b, l + 1, _StarlarkParser::expression_start);
    r = r && atAugAssignment(b, l + 1);
    p = r; // pin = 2
    r = r && report_error_(b, expression(b, l + 1));
    r = p && report_error_(b, aug_operator(b, l + 1)) && r;
    r = p && report_error_(b, expression(b, l + 1)) && r;
    r = p && statement_end(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // '+=' | '-=' | '*=' | '/=' | '//=' | '%=' | '&=' | '|=' | '^=' | '<<=' | '>>='
  static boolean aug_operator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "aug_operator")) return false;
    boolean r;
    r = consumeToken(b, PLUSEQ);
    if (!r) r = consumeToken(b, MINUSEQ);
    if (!r) r = consumeToken(b, MULTEQ);
    if (!r) r = consumeToken(b, DIVEQ);
    if (!r) r = consumeToken(b, FLOORDIVEQ);
    if (!r) r = consumeToken(b, PERCEQ);
    if (!r) r = consumeToken(b, ANDEQ);
    if (!r) r = consumeToken(b, OREQ);
    if (!r) r = consumeToken(b, XOREQ);
    if (!r) r = consumeToken(b, LTLTEQ);
    if (!r) r = consumeToken(b, GTGTEQ);
    return r;
  }

  /* ********************************************************** */
  // shift_level bitwise_and_tail*
  static boolean bitwise_and_level(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bitwise_and_level")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = shift_level(b, l + 1);
    r = r && bitwise_and_level_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // bitwise_and_tail*
  private static boolean bitwise_and_level_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bitwise_and_level_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!bitwise_and_tail(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "bitwise_and_level_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // '&' shift_level
  public static boolean bitwise_and_tail(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bitwise_and_tail")) return false;
    if (!nextTokenIsFast(b, AND)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, BINARY_EXPRESSION, null);
    r = consumeTokenFast(b, AND);
    p = r; // pin = 1
    r = r && shift_level(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // bitwise_xor_level bitwise_or_tail*
  static boolean bitwise_or_level(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bitwise_or_level")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<expression>");
    r = bitwise_xor_level(b, l + 1);
    r = r && bitwise_or_level_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // bitwise_or_tail*
  private static boolean bitwise_or_level_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bitwise_or_level_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!bitwise_or_tail(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "bitwise_or_level_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // '|' bitwise_xor_level
  public static boolean bitwise_or_tail(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bitwise_or_tail")) return false;
    if (!nextTokenIsFast(b, OR)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, BINARY_EXPRESSION, null);
    r = consumeTokenFast(b, OR);
    p = r; // pin = 1
    r = r && bitwise_xor_level(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // bitwise_and_level bitwise_xor_tail*
  static boolean bitwise_xor_level(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bitwise_xor_level")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = bitwise_and_level(b, l + 1);
    r = r && bitwise_xor_level_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // bitwise_xor_tail*
  private static boolean bitwise_xor_level_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bitwise_xor_level_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!bitwise_xor_tail(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "bitwise_xor_level_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // '^' bitwise_and_level
  public static boolean bitwise_xor_tail(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bitwise_xor_tail")) return false;
    if (!nextTokenIsFast(b, XOR)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, BINARY_EXPRESSION, null);
    r = consumeTokenFast(b, XOR);
    p = r; // pin = 1
    r = r && bitwise_and_level(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // !(DEDENT | <<eof>>) statement_or_break
  static boolean block_item(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_item")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = block_item_0(b, l + 1);
    p = r; // pin = 1
    r = r && statement_or_break(b, l + 1);
    exit_section_(b, l, m, r, p, _StarlarkParser::statement_recover);
    return r || p;
  }

  // !(DEDENT | <<eof>>)
  private static boolean block_item_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_item_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !block_item_0_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // DEDENT | <<eof>>
  private static boolean block_item_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_item_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, DEDENT);
    if (!r) r = eof(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // STATEMENT_BREAK statement_list
  static boolean block_suite(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_suite")) return false;
    if (!nextTokenIs(b, STATEMENT_BREAK)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, STATEMENT_BREAK);
    p = r; // pin = 1
    r = r && statement_list(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // 'break' statement_end
  public static boolean break_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "break_statement")) return false;
    if (!nextTokenIs(b, BREAK_KEYWORD)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, BREAK_STATEMENT, null);
    r = consumeToken(b, BREAK_KEYWORD);
    p = r; // pin = 1
    r = r && statement_end(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // argument_list
  public static boolean call_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "call_expression")) return false;
    if (!nextTokenIsFast(b, LPAR)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _LEFT_, CALL_EXPRESSION, null);
    r = argument_list(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // ')' | ']' | '}'
  static boolean close_bracket(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "close_bracket")) return false;
    boolean r;
    r = consumeToken(b, RPAR);
    if (!r) r = consumeToken(b, RBRACKET);
    if (!r) r = consumeToken(b, RBRACE);
    return r;
  }

  /* ********************************************************** */
  // <<expectColon>> suite
  static boolean colon_suite(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "colon_suite")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = expectColon(b, l + 1);
    p = r; // pin = 1
    r = r && suite(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // <<commaSeparator <<start>>>> !<<close>> <<item>>
  static boolean comma_tail(PsiBuilder b, int l, Parser _start, Parser _close, Parser _item) {
    if (!recursion_guard_(b, l, "comma_tail")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = commaSeparator(b, l + 1, _start);
    r = r && comma_tail_1(b, l + 1, _close);
    p = r; // pin = 2
    r = r && _item.parse(b, l);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // !<<close>>
  private static boolean comma_tail_1(PsiBuilder b, int l, Parser _close) {
    if (!recursion_guard_(b, l, "comma_tail_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !_close.parse(b, l);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // comparison_operand comparison_tail*
  static boolean comparison_level(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "comparison_level")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = comparison_operand(b, l + 1);
    r = r && comparison_level_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // comparison_tail*
  private static boolean comparison_level_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "comparison_level_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!comparison_tail(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "comparison_level_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // star_expression | bitwise_or_level
  static boolean comparison_operand(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "comparison_operand")) return false;
    boolean r;
    r = star_expression(b, l + 1);
    if (!r) r = bitwise_or_level(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // '==' | '!=' | '<=' | '>=' | '<' | '>' | 'in' | not_in_operator
  static boolean comparison_operator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "comparison_operator")) return false;
    boolean r;
    r = consumeTokenFast(b, EQEQ);
    if (!r) r = consumeTokenFast(b, NE);
    if (!r) r = consumeTokenFast(b, LE);
    if (!r) r = consumeTokenFast(b, GE);
    if (!r) r = consumeTokenFast(b, LT);
    if (!r) r = consumeTokenFast(b, GT);
    if (!r) r = consumeTokenFast(b, IN_KEYWORD);
    if (!r) r = not_in_operator(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // comparison_operator bitwise_or_level
  public static boolean comparison_tail(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "comparison_tail")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, BINARY_EXPRESSION, "<comparison tail>");
    r = comparison_operator(b, l + 1);
    p = r; // pin = 1
    r = r && bitwise_or_level(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // colon_suite <<blockDedent>>
  static boolean compound_suite(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "compound_suite")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = colon_suite(b, l + 1);
    r = r && blockDedent(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // comprehension_for+
  static boolean comprehension(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "comprehension")) return false;
    if (!nextTokenIs(b, FOR_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = comprehension_for(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!comprehension_for(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "comprehension", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // 'for' loop_targets 'in' range_test range_tuple? comprehension_if*
  static boolean comprehension_for(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "comprehension_for")) return false;
    if (!nextTokenIs(b, FOR_KEYWORD)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, FOR_KEYWORD);
    p = r; // pin = 1
    r = r && report_error_(b, loop_targets(b, l + 1));
    r = p && report_error_(b, consumeToken(b, IN_KEYWORD)) && r;
    r = p && report_error_(b, range_test(b, l + 1)) && r;
    r = p && report_error_(b, comprehension_for_4(b, l + 1)) && r;
    r = p && comprehension_for_5(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // range_tuple?
  private static boolean comprehension_for_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "comprehension_for_4")) return false;
    range_tuple(b, l + 1);
    return true;
  }

  // comprehension_if*
  private static boolean comprehension_for_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "comprehension_for_5")) return false;
    while (true) {
      int c = current_position_(b);
      if (!comprehension_if(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "comprehension_for_5", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // 'if' range_test
  static boolean comprehension_if(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "comprehension_if")) return false;
    if (!nextTokenIs(b, IF_KEYWORD)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, IF_KEYWORD);
    p = r; // pin = 1
    r = r && range_test(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // 'else'
  static boolean conditional_else(PsiBuilder b, int l) {
    return consumeToken(b, ELSE_KEYWORD);
  }

  /* ********************************************************** */
  // 'if' or_level conditional_else test
  public static boolean conditional_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "conditional_expression")) return false;
    if (!nextTokenIsFast(b, IF_KEYWORD)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, CONDITIONAL_EXPRESSION, null);
    r = consumeTokenFast(b, IF_KEYWORD);
    p = r; // pin = 1
    r = r && report_error_(b, or_level(b, l + 1));
    r = p && report_error_(b, conditional_else(b, l + 1)) && r;
    r = p && test(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // or_level conditional_expression?
  static boolean conditional_level(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "conditional_level")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = or_level(b, l + 1);
    r = r && conditional_level_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // conditional_expression?
  private static boolean conditional_level_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "conditional_level_1")) return false;
    conditional_expression(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // 'continue' statement_end
  public static boolean continue_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "continue_statement")) return false;
    if (!nextTokenIs(b, CONTINUE_KEYWORD)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, CONTINUE_STATEMENT, null);
    r = consumeToken(b, CONTINUE_KEYWORD);
    p = r; // pin = 1
    r = r && statement_end(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // comprehension
  public static boolean dict_comp_suffix(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dict_comp_suffix")) return false;
    if (!nextTokenIs(b, FOR_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_ | _UPPER_, DICT_COMP_EXPRESSION, null);
    r = comprehension(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // !'}' dict_item (dict_comp_suffix | <<comma_tail dict_entry_start '}' dict_item>>* ','?)
  static boolean dict_content(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dict_content")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = dict_content_0(b, l + 1);
    p = r; // pin = 1
    r = r && report_error_(b, dict_item(b, l + 1));
    r = p && dict_content_2(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // !'}'
  private static boolean dict_content_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dict_content_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !consumeToken(b, RBRACE);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // dict_comp_suffix | <<comma_tail dict_entry_start '}' dict_item>>* ','?
  private static boolean dict_content_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dict_content_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = dict_comp_suffix(b, l + 1);
    if (!r) r = dict_content_2_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // <<comma_tail dict_entry_start '}' dict_item>>* ','?
  private static boolean dict_content_2_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dict_content_2_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = dict_content_2_1_0(b, l + 1);
    r = r && dict_content_2_1_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // <<comma_tail dict_entry_start '}' dict_item>>*
  private static boolean dict_content_2_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dict_content_2_1_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!comma_tail(b, l + 1, _StarlarkParser::dict_entry_start, RBRACE_parser_, _StarlarkParser::dict_item)) break;
      if (!empty_element_parsed_guard_(b, "dict_content_2_1_0", c)) break;
    }
    return true;
  }

  // ','?
  private static boolean dict_content_2_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dict_content_2_1_1")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // double_star_expression | key_value_expression
  static boolean dict_entry(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dict_entry")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<dict entry>");
    r = double_star_expression(b, l + 1);
    if (!r) r = key_value_expression(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // expression_start | '**'
  static boolean dict_entry_start(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dict_entry_start")) return false;
    boolean r;
    r = expression_start(b, l + 1);
    if (!r) r = consumeToken(b, EXP);
    return r;
  }

  /* ********************************************************** */
  // dict_entry
  static boolean dict_item(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dict_item")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<dict entry>");
    r = dict_entry(b, l + 1);
    exit_section_(b, l, m, r, false, _StarlarkParser::dict_recover);
    return r;
  }

  /* ********************************************************** */
  // '{' dict_content? '}'
  public static boolean dict_literal_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dict_literal_expression")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, DICT_LITERAL_EXPRESSION, null);
    r = consumeToken(b, LBRACE);
    p = r; // pin = 1
    r = r && report_error_(b, dict_literal_expression_1(b, l + 1));
    r = p && consumeToken(b, RBRACE) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // dict_content?
  private static boolean dict_literal_expression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dict_literal_expression_1")) return false;
    dict_content(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // !(',' | close_bracket | 'for' | header_colon | STATEMENT_BREAK | dict_entry_start)
  static boolean dict_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dict_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !dict_recover_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ',' | close_bracket | 'for' | header_colon | STATEMENT_BREAK | dict_entry_start
  private static boolean dict_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dict_recover_0")) return false;
    boolean r;
    r = consumeToken(b, COMMA);
    if (!r) r = close_bracket(b, l + 1);
    if (!r) r = consumeToken(b, FOR_KEYWORD);
    if (!r) r = header_colon(b, l + 1);
    if (!r) r = consumeToken(b, STATEMENT_BREAK);
    if (!r) r = dict_entry_start(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // '**' bitwise_or_level
  public static boolean double_star_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "double_star_expression")) return false;
    if (!nextTokenIs(b, EXP)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, DOUBLE_STAR_EXPRESSION, null);
    r = consumeToken(b, EXP);
    p = r; // pin = 1
    r = r && bitwise_or_level(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // 'elif' test compound_suite
  static boolean elif_clause(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "elif_clause")) return false;
    if (!nextTokenIs(b, ELIF_KEYWORD)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, ELIF_KEYWORD);
    p = r; // pin = 1
    r = r && report_error_(b, test(b, l + 1));
    r = p && compound_suite(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // 'else' compound_suite
  static boolean else_clause(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "else_clause")) return false;
    if (!nextTokenIs(b, ELSE_KEYWORD)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, ELSE_KEYWORD);
    p = r; // pin = 1
    r = r && compound_suite(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // &(':' | ']')
  public static boolean empty_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "empty_expression")) return false;
    if (!nextTokenIs(b, "<empty expression>", COLON, RBRACKET)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_, EMPTY_EXPRESSION, "<empty expression>");
    r = empty_expression_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ':' | ']'
  private static boolean empty_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "empty_expression_0")) return false;
    boolean r;
    r = consumeToken(b, COLON);
    if (!r) r = consumeToken(b, RBRACKET);
    return r;
  }

  /* ********************************************************** */
  // &')'
  public static boolean empty_tuple(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "empty_tuple")) return false;
    if (!nextTokenIs(b, RPAR)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_ | _UPPER_, TUPLE_EXPRESSION, null);
    r = consumeToken(b, RPAR);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // test tuple_expression?
  static boolean expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<expression>");
    r = test(b, l + 1);
    r = r && expression_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // tuple_expression?
  private static boolean expression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_1")) return false;
    tuple_expression(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // IDENTIFIER | INT | FLOAT | STRING | BYTES | 'None' | 'True' | 'False'
  //   | '(' | '[' | '{' | 'lambda' | 'not' | '-' | '+' | '~' | '*'
  static boolean expression_start(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_start")) return false;
    boolean r;
    r = consumeToken(b, IDENTIFIER);
    if (!r) r = consumeToken(b, INT);
    if (!r) r = consumeToken(b, FLOAT);
    if (!r) r = consumeToken(b, STRING);
    if (!r) r = consumeToken(b, BYTES);
    if (!r) r = consumeToken(b, NONE_KEYWORD);
    if (!r) r = consumeToken(b, TRUE_KEYWORD);
    if (!r) r = consumeToken(b, FALSE_KEYWORD);
    if (!r) r = consumeToken(b, LPAR);
    if (!r) r = consumeToken(b, LBRACKET);
    if (!r) r = consumeToken(b, LBRACE);
    if (!r) r = consumeToken(b, LAMBDA_KEYWORD);
    if (!r) r = consumeToken(b, NOT_KEYWORD);
    if (!r) r = consumeToken(b, MINUS);
    if (!r) r = consumeToken(b, PLUS);
    if (!r) r = consumeToken(b, TILDE);
    if (!r) r = consumeToken(b, MULT);
    return r;
  }

  /* ********************************************************** */
  // expression statement_end
  public static boolean expression_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_statement")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, EXPRESSION_STATEMENT, "<expression statement>");
    r = expression(b, l + 1);
    p = r; // pin = 1
    r = r && statement_end(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // 'False'
  public static boolean false_literal_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "false_literal_expression")) return false;
    if (!nextTokenIs(b, FALSE_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, FALSE_KEYWORD);
    exit_section_(b, m, FALSE_LITERAL_EXPRESSION, r);
    return r;
  }

  /* ********************************************************** */
  // !<<eof>> statement_or_break
  static boolean file_item(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "file_item")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = file_item_0(b, l + 1);
    p = r; // pin = 1
    r = r && statement_or_break(b, l + 1);
    exit_section_(b, l, m, r, p, _StarlarkParser::statement_recover);
    return r || p;
  }

  // !<<eof>>
  private static boolean file_item_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "file_item_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !eof(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // load_string
  public static boolean filename_load_value(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "filename_load_value")) return false;
    if (!nextTokenIs(b, STRING)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = load_string(b, l + 1);
    exit_section_(b, m, FILENAME_LOAD_VALUE, r);
    return r;
  }

  /* ********************************************************** */
  // FLOAT
  public static boolean float_literal_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "float_literal_expression")) return false;
    if (!nextTokenIs(b, FLOAT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, FLOAT);
    exit_section_(b, m, FLOAT_LITERAL_EXPRESSION, r);
    return r;
  }

  /* ********************************************************** */
  // 'for' loop_targets 'in' expression compound_suite
  public static boolean for_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "for_statement")) return false;
    if (!nextTokenIs(b, FOR_KEYWORD)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, FOR_STATEMENT, null);
    r = consumeToken(b, FOR_KEYWORD);
    p = r; // pin = 1
    r = r && report_error_(b, loop_targets(b, l + 1));
    r = p && report_error_(b, consumeToken(b, IN_KEYWORD)) && r;
    r = p && report_error_(b, expression(b, l + 1)) && r;
    r = p && compound_suite(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // 'def' IDENTIFIER parameter_list colon_suite
  public static boolean function_declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function_declaration")) return false;
    if (!nextTokenIs(b, DEF_KEYWORD)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, FUNCTION_DECLARATION, null);
    r = consumeTokens(b, 1, DEF_KEYWORD, IDENTIFIER);
    p = r; // pin = 1
    r = r && report_error_(b, parameter_list(b, l + 1));
    r = p && colon_suite(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // function_declaration <<blockDedent>>
  static boolean function_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function_statement")) return false;
    if (!nextTokenIs(b, DEF_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = function_declaration(b, l + 1);
    r = r && blockDedent(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // generator_for+
  static boolean generator_comprehension(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "generator_comprehension")) return false;
    if (!nextTokenIs(b, FOR_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = generator_for(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!generator_for(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "generator_comprehension", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // 'for' loop_targets 'in' or_level comprehension_if*
  static boolean generator_for(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "generator_for")) return false;
    if (!nextTokenIs(b, FOR_KEYWORD)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, FOR_KEYWORD);
    p = r; // pin = 1
    r = r && report_error_(b, loop_targets(b, l + 1));
    r = p && report_error_(b, consumeToken(b, IN_KEYWORD)) && r;
    r = p && report_error_(b, or_level(b, l + 1)) && r;
    r = p && generator_for_4(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // comprehension_if*
  private static boolean generator_for_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "generator_for_4")) return false;
    while (true) {
      int c = current_position_(b);
      if (!comprehension_if(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "generator_for_4", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // generator_comprehension
  public static boolean generator_suffix(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "generator_suffix")) return false;
    if (!nextTokenIs(b, FOR_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _UPPER_, GENERATOR_EXPRESSION, null);
    r = generator_comprehension(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // ':' &STATEMENT_BREAK
  static boolean header_colon(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "header_colon")) return false;
    if (!nextTokenIs(b, COLON)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COLON);
    r = r && header_colon_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // &STATEMENT_BREAK
  private static boolean header_colon_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "header_colon_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = consumeToken(b, STATEMENT_BREAK);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // 'if' test compound_suite elif_clause* else_clause?
  public static boolean if_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "if_statement")) return false;
    if (!nextTokenIs(b, IF_KEYWORD)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, IF_STATEMENT, null);
    r = consumeToken(b, IF_KEYWORD);
    p = r; // pin = 1
    r = r && report_error_(b, test(b, l + 1));
    r = p && report_error_(b, compound_suite(b, l + 1)) && r;
    r = p && report_error_(b, if_statement_3(b, l + 1)) && r;
    r = p && if_statement_4(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // elif_clause*
  private static boolean if_statement_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "if_statement_3")) return false;
    while (true) {
      int c = current_position_(b);
      if (!elif_clause(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "if_statement_3", c)) break;
    }
    return true;
  }

  // else_clause?
  private static boolean if_statement_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "if_statement_4")) return false;
    else_clause(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // INDENT block_item* <<blockEnd>>
  static boolean indented_statements(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "indented_statements")) return false;
    if (!nextTokenIs(b, INDENT)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, INDENT);
    p = r; // pin = 1
    r = r && report_error_(b, indented_statements_1(b, l + 1));
    r = p && blockEnd(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // block_item*
  private static boolean indented_statements_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "indented_statements_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!block_item(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "indented_statements_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // <<inlineSuite inline_statements>> | <<statementExpected>> <<inlineSuite inline_statements>>?
  public static boolean inline_statement_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inline_statement_list")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, STATEMENT_LIST, "<statement>");
    r = inlineSuite(b, l + 1, _StarlarkParser::inline_statements);
    if (!r) r = inline_statement_list_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // <<statementExpected>> <<inlineSuite inline_statements>>?
  private static boolean inline_statement_list_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inline_statement_list_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = statementExpected(b, l + 1);
    r = r && inline_statement_list_1_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // <<inlineSuite inline_statements>>?
  private static boolean inline_statement_list_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inline_statement_list_1_1")) return false;
    inlineSuite(b, l + 1, _StarlarkParser::inline_statements);
    return true;
  }

  /* ********************************************************** */
  // simple_statement (';' simple_statement)* (';' STATEMENT_BREAK?)?
  static boolean inline_statements(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inline_statements")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = simple_statement(b, l + 1);
    r = r && inline_statements_1(b, l + 1);
    r = r && inline_statements_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (';' simple_statement)*
  private static boolean inline_statements_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inline_statements_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!inline_statements_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "inline_statements_1", c)) break;
    }
    return true;
  }

  // ';' simple_statement
  private static boolean inline_statements_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inline_statements_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, SEMICOLON);
    r = r && simple_statement(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (';' STATEMENT_BREAK?)?
  private static boolean inline_statements_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inline_statements_2")) return false;
    inline_statements_2_0(b, l + 1);
    return true;
  }

  // ';' STATEMENT_BREAK?
  private static boolean inline_statements_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inline_statements_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, SEMICOLON);
    r = r && inline_statements_2_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // STATEMENT_BREAK?
  private static boolean inline_statements_2_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inline_statements_2_0_1")) return false;
    consumeToken(b, STATEMENT_BREAK);
    return true;
  }

  /* ********************************************************** */
  // INT
  public static boolean integer_literal_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "integer_literal_expression")) return false;
    if (!nextTokenIs(b, INT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, INT);
    exit_section_(b, m, INTEGER_LITERAL_EXPRESSION, r);
    return r;
  }

  /* ********************************************************** */
  // test ':' test
  public static boolean key_value_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "key_value_expression")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, KEY_VALUE_EXPRESSION, "<key value expression>");
    r = test(b, l + 1);
    p = r; // pin = 1
    r = r && report_error_(b, consumeToken(b, COLON));
    r = p && test(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // '='
  static boolean keyword_equals(PsiBuilder b, int l) {
    return consumeTokenFast(b, EQ);
  }

  /* ********************************************************** */
  // '*' &','
  public static boolean keyword_only_boundary(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "keyword_only_boundary")) return false;
    if (!nextTokenIs(b, MULT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, MULT);
    r = r && keyword_only_boundary_1(b, l + 1);
    exit_section_(b, m, KEYWORD_ONLY_BOUNDARY, r);
    return r;
  }

  // &','
  private static boolean keyword_only_boundary_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "keyword_only_boundary_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = consumeToken(b, COMMA);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // IDENTIFIER keyword_equals
  static boolean keyword_prefix(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "keyword_prefix")) return false;
    if (!nextTokenIsFast(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenFast(b, IDENTIFIER);
    r = r && keyword_equals(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '**' IDENTIFIER
  public static boolean keyword_variadic_parameter(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "keyword_variadic_parameter")) return false;
    if (!nextTokenIs(b, EXP)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, KEYWORD_VARIADIC_PARAMETER, null);
    r = consumeTokens(b, 1, EXP, IDENTIFIER);
    p = r; // pin = 1
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // 'lambda' lambda_parameter_list ':' test
  public static boolean lambda_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "lambda_expression")) return false;
    if (!nextTokenIs(b, LAMBDA_KEYWORD)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, LAMBDA_EXPRESSION, null);
    r = consumeToken(b, LAMBDA_KEYWORD);
    p = r; // pin = 1
    r = r && report_error_(b, lambda_parameter_list(b, l + 1));
    r = p && report_error_(b, consumeToken(b, COLON)) && r;
    r = p && test(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // lambda_parameters?
  public static boolean lambda_parameter_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "lambda_parameter_list")) return false;
    Marker m = enter_section_(b, l, _NONE_, PARAMETER_LIST, "<lambda parameter list>");
    lambda_parameters(b, l + 1);
    exit_section_(b, l, m, true, false, null);
    return true;
  }

  /* ********************************************************** */
  // !':' parameter_item <<comma_tail parameter_start ':' parameter_item>>* ','?
  static boolean lambda_parameters(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "lambda_parameters")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = lambda_parameters_0(b, l + 1);
    p = r; // pin = 1
    r = r && report_error_(b, parameter_item(b, l + 1));
    r = p && report_error_(b, lambda_parameters_2(b, l + 1)) && r;
    r = p && lambda_parameters_3(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // !':'
  private static boolean lambda_parameters_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "lambda_parameters_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !consumeToken(b, COLON);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // <<comma_tail parameter_start ':' parameter_item>>*
  private static boolean lambda_parameters_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "lambda_parameters_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!comma_tail(b, l + 1, _StarlarkParser::parameter_start, COLON_parser_, _StarlarkParser::parameter_item)) break;
      if (!empty_element_parsed_guard_(b, "lambda_parameters_2", c)) break;
    }
    return true;
  }

  // ','?
  private static boolean lambda_parameters_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "lambda_parameters_3")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // comprehension
  public static boolean list_comp_suffix(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "list_comp_suffix")) return false;
    if (!nextTokenIs(b, FOR_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_ | _UPPER_, LIST_COMP_EXPRESSION, null);
    r = comprehension(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // !']' list_item (list_comp_suffix | <<comma_tail expression_start ']' list_item>>* ','?)
  static boolean list_content(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "list_content")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = list_content_0(b, l + 1);
    p = r; // pin = 1
    r = r && report_error_(b, list_item(b, l + 1));
    r = p && list_content_2(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // !']'
  private static boolean list_content_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "list_content_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !consumeToken(b, RBRACKET);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // list_comp_suffix | <<comma_tail expression_start ']' list_item>>* ','?
  private static boolean list_content_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "list_content_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = list_comp_suffix(b, l + 1);
    if (!r) r = list_content_2_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // <<comma_tail expression_start ']' list_item>>* ','?
  private static boolean list_content_2_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "list_content_2_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = list_content_2_1_0(b, l + 1);
    r = r && list_content_2_1_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // <<comma_tail expression_start ']' list_item>>*
  private static boolean list_content_2_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "list_content_2_1_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!comma_tail(b, l + 1, _StarlarkParser::expression_start, RBRACKET_parser_, _StarlarkParser::list_item)) break;
      if (!empty_element_parsed_guard_(b, "list_content_2_1_0", c)) break;
    }
    return true;
  }

  // ','?
  private static boolean list_content_2_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "list_content_2_1_1")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // test
  static boolean list_item(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "list_item")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<expression>");
    r = test(b, l + 1);
    exit_section_(b, l, m, r, false, _StarlarkParser::list_recover);
    return r;
  }

  /* ********************************************************** */
  // '[' list_content? ']'
  public static boolean list_literal_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "list_literal_expression")) return false;
    if (!nextTokenIs(b, LBRACKET)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, LIST_LITERAL_EXPRESSION, null);
    r = consumeToken(b, LBRACKET);
    p = r; // pin = 1
    r = r && report_error_(b, list_literal_expression_1(b, l + 1));
    r = p && consumeToken(b, RBRACKET) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // list_content?
  private static boolean list_literal_expression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "list_literal_expression_1")) return false;
    list_content(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // !(',' | close_bracket | 'for' | header_colon | STATEMENT_BREAK | expression_start)
  static boolean list_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "list_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !list_recover_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ',' | close_bracket | 'for' | header_colon | STATEMENT_BREAK | expression_start
  private static boolean list_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "list_recover_0")) return false;
    boolean r;
    r = consumeToken(b, COMMA);
    if (!r) r = close_bracket(b, l + 1);
    if (!r) r = consumeToken(b, FOR_KEYWORD);
    if (!r) r = header_colon(b, l + 1);
    if (!r) r = consumeToken(b, STATEMENT_BREAK);
    if (!r) r = expression_start(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // filename_load_value
  static boolean load_filename_item(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "load_filename_item")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<string>");
    r = filename_load_value(b, l + 1);
    exit_section_(b, l, m, r, false, _StarlarkParser::load_recover);
    return r;
  }

  /* ********************************************************** */
  // named_load_value | string_load_value
  static boolean load_item(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "load_item")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<string>");
    r = named_load_value(b, l + 1);
    if (!r) r = string_load_value(b, l + 1);
    exit_section_(b, l, m, r, false, _StarlarkParser::load_recover);
    return r;
  }

  /* ********************************************************** */
  // !(',' | open_bracket | close_bracket | STATEMENT_BREAK | load_value_start)
  static boolean load_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "load_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !load_recover_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ',' | open_bracket | close_bracket | STATEMENT_BREAK | load_value_start
  private static boolean load_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "load_recover_0")) return false;
    boolean r;
    r = consumeToken(b, COMMA);
    if (!r) r = open_bracket(b, l + 1);
    if (!r) r = close_bracket(b, l + 1);
    if (!r) r = consumeToken(b, STATEMENT_BREAK);
    if (!r) r = load_value_start(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // 'load' '(' load_values? ')' statement_end
  public static boolean load_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "load_statement")) return false;
    if (!nextTokenIs(b, LOAD_KEYWORD)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, LOAD_STATEMENT, null);
    r = consumeTokens(b, 1, LOAD_KEYWORD, LPAR);
    p = r; // pin = 1
    r = r && report_error_(b, load_statement_2(b, l + 1));
    r = p && report_error_(b, consumeToken(b, RPAR)) && r;
    r = p && statement_end(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // load_values?
  private static boolean load_statement_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "load_statement_2")) return false;
    load_values(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // &STRING string_literal_expression
  static boolean load_string(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "load_string")) return false;
    if (!nextTokenIs(b, "<string>", STRING)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<string>");
    r = load_string_0(b, l + 1);
    r = r && string_literal_expression(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // &STRING
  private static boolean load_string_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "load_string_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = consumeToken(b, STRING);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // STRING | IDENTIFIER '='
  static boolean load_value_start(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "load_value_start")) return false;
    if (!nextTokenIs(b, "", IDENTIFIER, STRING)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, STRING);
    if (!r) r = parseTokens(b, 0, IDENTIFIER, EQ);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // !')' load_filename_item <<comma_tail load_value_start ')' load_item>>* ','?
  static boolean load_values(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "load_values")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = load_values_0(b, l + 1);
    p = r; // pin = 1
    r = r && report_error_(b, load_filename_item(b, l + 1));
    r = p && report_error_(b, load_values_2(b, l + 1)) && r;
    r = p && load_values_3(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // !')'
  private static boolean load_values_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "load_values_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !consumeToken(b, RPAR);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // <<comma_tail load_value_start ')' load_item>>*
  private static boolean load_values_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "load_values_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!comma_tail(b, l + 1, _StarlarkParser::load_value_start, RPAR_parser_, _StarlarkParser::load_item)) break;
      if (!empty_element_parsed_guard_(b, "load_values_2", c)) break;
    }
    return true;
  }

  // ','?
  private static boolean load_values_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "load_values_3")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // star_expression | bitwise_or_level
  static boolean loop_target(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "loop_target")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<expression>");
    r = star_expression(b, l + 1);
    if (!r) r = bitwise_or_level(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // loop_target loop_target_tuple?
  static boolean loop_target_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "loop_target_list")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = loop_target(b, l + 1);
    r = r && loop_target_list_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // loop_target_tuple?
  private static boolean loop_target_list_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "loop_target_list_1")) return false;
    loop_target_tuple(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // ',' (loop_target (',' loop_target)* ','?)?
  public static boolean loop_target_tuple(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "loop_target_tuple")) return false;
    if (!nextTokenIs(b, COMMA)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _LEFT_, TUPLE_EXPRESSION, null);
    r = consumeToken(b, COMMA);
    r = r && loop_target_tuple_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (loop_target (',' loop_target)* ','?)?
  private static boolean loop_target_tuple_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "loop_target_tuple_1")) return false;
    loop_target_tuple_1_0(b, l + 1);
    return true;
  }

  // loop_target (',' loop_target)* ','?
  private static boolean loop_target_tuple_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "loop_target_tuple_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = loop_target(b, l + 1);
    r = r && loop_target_tuple_1_0_1(b, l + 1);
    r = r && loop_target_tuple_1_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (',' loop_target)*
  private static boolean loop_target_tuple_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "loop_target_tuple_1_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!loop_target_tuple_1_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "loop_target_tuple_1_0_1", c)) break;
    }
    return true;
  }

  // ',' loop_target
  private static boolean loop_target_tuple_1_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "loop_target_tuple_1_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && loop_target(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ','?
  private static boolean loop_target_tuple_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "loop_target_tuple_1_0_2")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // <<targetMode loop_target_list>>
  static boolean loop_targets(PsiBuilder b, int l) {
    return targetMode(b, l + 1, _StarlarkParser::loop_target_list);
  }

  /* ********************************************************** */
  // IDENTIFIER
  public static boolean mandatory_parameter(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "mandatory_parameter")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IDENTIFIER);
    exit_section_(b, m, MANDATORY_PARAMETER, r);
    return r;
  }

  /* ********************************************************** */
  // <<memberChain primary_level>> member_tail*
  static boolean member_level(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "member_level")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = memberChain(b, l + 1, _StarlarkParser::primary_level);
    r = r && member_level_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // member_tail*
  private static boolean member_level_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "member_level_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!member_tail(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "member_level_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // reference_expression | call_expression | subscription_expression
  static boolean member_tail(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "member_tail")) return false;
    boolean r;
    r = reference_expression(b, l + 1);
    if (!r) r = call_expression(b, l + 1);
    if (!r) r = subscription_expression(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // unary_level multiplicative_tail*
  static boolean multiplicative_level(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "multiplicative_level")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = unary_level(b, l + 1);
    r = r && multiplicative_level_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // multiplicative_tail*
  private static boolean multiplicative_level_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "multiplicative_level_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!multiplicative_tail(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "multiplicative_level_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // ('*' | '//' | '/' | '%') unary_level
  public static boolean multiplicative_tail(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "multiplicative_tail")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, BINARY_EXPRESSION, "<multiplicative tail>");
    r = multiplicative_tail_0(b, l + 1);
    p = r; // pin = 1
    r = r && unary_level(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // '*' | '//' | '/' | '%'
  private static boolean multiplicative_tail_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "multiplicative_tail_0")) return false;
    boolean r;
    r = consumeTokenFast(b, MULT);
    if (!r) r = consumeTokenFast(b, FLOORDIV);
    if (!r) r = consumeTokenFast(b, DIV);
    if (!r) r = consumeTokenFast(b, PERC);
    return r;
  }

  /* ********************************************************** */
  // keyword_prefix test
  public static boolean named_argument_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "named_argument_expression")) return false;
    if (!nextTokenIsFast(b, IDENTIFIER)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, NAMED_ARGUMENT_EXPRESSION, null);
    r = keyword_prefix(b, l + 1);
    p = r; // pin = 1
    r = r && test(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // keyword_prefix load_string
  public static boolean named_load_value(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "named_load_value")) return false;
    if (!nextTokenIsFast(b, IDENTIFIER)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, NAMED_LOAD_VALUE, null);
    r = keyword_prefix(b, l + 1);
    p = r; // pin = 1
    r = r && load_string(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // 'None'
  public static boolean none_literal_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "none_literal_expression")) return false;
    if (!nextTokenIs(b, NONE_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, NONE_KEYWORD);
    exit_section_(b, m, NONE_LITERAL_EXPRESSION, r);
    return r;
  }

  /* ********************************************************** */
  // 'in'
  static boolean not_in_keyword(PsiBuilder b, int l) {
    return consumeTokenFast(b, IN_KEYWORD);
  }

  /* ********************************************************** */
  // 'not' not_in_keyword
  static boolean not_in_operator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "not_in_operator")) return false;
    if (!nextTokenIsFast(b, NOT_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenFast(b, NOT_KEYWORD);
    r = r && not_in_keyword(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '(' | '[' | '{'
  static boolean open_bracket(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "open_bracket")) return false;
    boolean r;
    r = consumeToken(b, LPAR);
    if (!r) r = consumeToken(b, LBRACKET);
    if (!r) r = consumeToken(b, LBRACE);
    return r;
  }

  /* ********************************************************** */
  // keyword_prefix test
  public static boolean optional_parameter(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "optional_parameter")) return false;
    if (!nextTokenIsFast(b, IDENTIFIER)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, OPTIONAL_PARAMETER, null);
    r = keyword_prefix(b, l + 1);
    p = r; // pin = 1
    r = r && test(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // and_level or_tail*
  static boolean or_level(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "or_level")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<expression>");
    r = and_level(b, l + 1);
    r = r && or_level_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // or_tail*
  private static boolean or_level_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "or_level_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!or_tail(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "or_level_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // 'or' and_level
  public static boolean or_tail(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "or_tail")) return false;
    if (!nextTokenIsFast(b, OR_KEYWORD)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, BINARY_EXPRESSION, null);
    r = consumeTokenFast(b, OR_KEYWORD);
    p = r; // pin = 1
    r = r && and_level(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // variadic_parameter
  //   | keyword_only_boundary
  //   | keyword_variadic_parameter
  //   | optional_parameter
  //   | mandatory_parameter
  static boolean parameter(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<parameter>");
    r = variadic_parameter(b, l + 1);
    if (!r) r = keyword_only_boundary(b, l + 1);
    if (!r) r = keyword_variadic_parameter(b, l + 1);
    if (!r) r = optional_parameter(b, l + 1);
    if (!r) r = mandatory_parameter(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // parameter
  static boolean parameter_item(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_item")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<parameter>");
    r = parameter(b, l + 1);
    exit_section_(b, l, m, r, false, _StarlarkParser::parameter_recover);
    return r;
  }

  /* ********************************************************** */
  // '(' parameters? ')'
  public static boolean parameter_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_list")) return false;
    if (!nextTokenIs(b, LPAR)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, PARAMETER_LIST, null);
    r = consumeToken(b, LPAR);
    p = r; // pin = 1
    r = r && report_error_(b, parameter_list_1(b, l + 1));
    r = p && consumeToken(b, RPAR) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // parameters?
  private static boolean parameter_list_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_list_1")) return false;
    parameters(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // !(',' | open_bracket | close_bracket | ':' | STATEMENT_BREAK | parameter_start)
  static boolean parameter_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !parameter_recover_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ',' | open_bracket | close_bracket | ':' | STATEMENT_BREAK | parameter_start
  private static boolean parameter_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_recover_0")) return false;
    boolean r;
    r = consumeToken(b, COMMA);
    if (!r) r = open_bracket(b, l + 1);
    if (!r) r = close_bracket(b, l + 1);
    if (!r) r = consumeToken(b, COLON);
    if (!r) r = consumeToken(b, STATEMENT_BREAK);
    if (!r) r = parameter_start(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // IDENTIFIER | '*' | '**'
  static boolean parameter_start(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_start")) return false;
    boolean r;
    r = consumeToken(b, IDENTIFIER);
    if (!r) r = consumeToken(b, MULT);
    if (!r) r = consumeToken(b, EXP);
    return r;
  }

  /* ********************************************************** */
  // !')' parameter_item <<comma_tail parameter_start ')' parameter_item>>* ','?
  static boolean parameters(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameters")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = parameters_0(b, l + 1);
    p = r; // pin = 1
    r = r && report_error_(b, parameter_item(b, l + 1));
    r = p && report_error_(b, parameters_2(b, l + 1)) && r;
    r = p && parameters_3(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // !')'
  private static boolean parameters_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameters_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !consumeToken(b, RPAR);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // <<comma_tail parameter_start ')' parameter_item>>*
  private static boolean parameters_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameters_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!comma_tail(b, l + 1, _StarlarkParser::parameter_start, RPAR_parser_, _StarlarkParser::parameter_item)) break;
      if (!empty_element_parsed_guard_(b, "parameters_2", c)) break;
    }
    return true;
  }

  // ','?
  private static boolean parameters_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameters_3")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // empty_tuple | expression generator_suffix?
  static boolean parenthesized_content(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parenthesized_content")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_);
    r = empty_tuple(b, l + 1);
    if (!r) r = parenthesized_content_1(b, l + 1);
    exit_section_(b, l, m, r, false, _StarlarkParser::parenthesized_recover);
    return r;
  }

  // expression generator_suffix?
  private static boolean parenthesized_content_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parenthesized_content_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expression(b, l + 1);
    r = r && parenthesized_content_1_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // generator_suffix?
  private static boolean parenthesized_content_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parenthesized_content_1_1")) return false;
    generator_suffix(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // '(' parenthesized_content ')'
  public static boolean parenthesized_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parenthesized_expression")) return false;
    if (!nextTokenIs(b, LPAR)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, PARENTHESIZED_EXPRESSION, null);
    r = consumeToken(b, LPAR);
    p = r; // pin = 1
    r = r && report_error_(b, parenthesized_content(b, l + 1));
    r = p && consumeToken(b, RPAR) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // !(')' | header_colon | STATEMENT_BREAK)
  static boolean parenthesized_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parenthesized_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !parenthesized_recover_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ')' | header_colon | STATEMENT_BREAK
  private static boolean parenthesized_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parenthesized_recover_0")) return false;
    boolean r;
    r = consumeToken(b, RPAR);
    if (!r) r = header_colon(b, l + 1);
    if (!r) r = consumeToken(b, STATEMENT_BREAK);
    return r;
  }

  /* ********************************************************** */
  // 'pass' statement_end
  public static boolean pass_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pass_statement")) return false;
    if (!nextTokenIs(b, PASS_KEYWORD)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, PASS_STATEMENT, null);
    r = consumeToken(b, PASS_KEYWORD);
    p = r; // pin = 1
    r = r && statement_end(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // member_level power_tail?
  static boolean power_level(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "power_level")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = member_level(b, l + 1);
    r = r && power_level_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // power_tail?
  private static boolean power_level_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "power_level_1")) return false;
    power_tail(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // '**' unary_level
  public static boolean power_tail(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "power_tail")) return false;
    if (!nextTokenIsFast(b, EXP)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, BINARY_EXPRESSION, null);
    r = consumeTokenFast(b, EXP);
    p = r; // pin = 1
    r = r && unary_level(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // ('+' | '-' | '~' | 'not') unary_level
  public static boolean prefix_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "prefix_expression")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _COLLAPSE_, PREFIX_EXPRESSION, "<prefix expression>");
    r = prefix_expression_0(b, l + 1);
    p = r; // pin = 1
    r = r && unary_level(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // '+' | '-' | '~' | 'not'
  private static boolean prefix_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "prefix_expression_0")) return false;
    boolean r;
    r = consumeToken(b, PLUS);
    if (!r) r = consumeToken(b, MINUS);
    if (!r) r = consumeToken(b, TILDE);
    if (!r) r = consumeToken(b, NOT_KEYWORD);
    return r;
  }

  /* ********************************************************** */
  // <<nameExpression>>
  //   | integer_literal_expression
  //   | float_literal_expression
  //   | string_literal_expression
  //   | none_literal_expression
  //   | true_literal_expression
  //   | false_literal_expression
  //   | parenthesized_expression
  //   | list_literal_expression
  //   | dict_literal_expression
  static boolean primary_level(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "primary_level")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = nameExpression(b, l + 1);
    if (!r) r = integer_literal_expression(b, l + 1);
    if (!r) r = float_literal_expression(b, l + 1);
    if (!r) r = string_literal_expression(b, l + 1);
    if (!r) r = none_literal_expression(b, l + 1);
    if (!r) r = true_literal_expression(b, l + 1);
    if (!r) r = false_literal_expression(b, l + 1);
    if (!r) r = parenthesized_expression(b, l + 1);
    if (!r) r = list_literal_expression(b, l + 1);
    if (!r) r = dict_literal_expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // lambda_expression | or_level
  static boolean range_test(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "range_test")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<expression>");
    r = lambda_expression(b, l + 1);
    if (!r) r = or_level(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // ',' range_test? (',' range_test)* ','?
  public static boolean range_tuple(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "range_tuple")) return false;
    if (!nextTokenIs(b, COMMA)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _LEFT_, TUPLE_EXPRESSION, null);
    r = consumeToken(b, COMMA);
    r = r && range_tuple_1(b, l + 1);
    r = r && range_tuple_2(b, l + 1);
    r = r && range_tuple_3(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // range_test?
  private static boolean range_tuple_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "range_tuple_1")) return false;
    range_test(b, l + 1);
    return true;
  }

  // (',' range_test)*
  private static boolean range_tuple_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "range_tuple_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!range_tuple_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "range_tuple_2", c)) break;
    }
    return true;
  }

  // ',' range_test
  private static boolean range_tuple_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "range_tuple_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && range_test(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ','?
  private static boolean range_tuple_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "range_tuple_3")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // '.' reference_name attribute_target?
  public static boolean reference_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "reference_expression")) return false;
    if (!nextTokenIsFast(b, DOT)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, REFERENCE_EXPRESSION, null);
    r = consumeTokenFast(b, DOT);
    p = r; // pin = 1
    r = r && report_error_(b, reference_name(b, l + 1));
    r = p && reference_expression_2(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // attribute_target?
  private static boolean reference_expression_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "reference_expression_2")) return false;
    attribute_target(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // IDENTIFIER
  static boolean reference_name(PsiBuilder b, int l) {
    return consumeToken(b, IDENTIFIER);
  }

  /* ********************************************************** */
  // 'return' expression? statement_end
  public static boolean return_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "return_statement")) return false;
    if (!nextTokenIs(b, RETURN_KEYWORD)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, RETURN_STATEMENT, null);
    r = consumeToken(b, RETURN_KEYWORD);
    p = r; // pin = 1
    r = r && report_error_(b, return_statement_1(b, l + 1));
    r = p && statement_end(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // expression?
  private static boolean return_statement_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "return_statement_1")) return false;
    expression(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // additive_level shift_tail*
  static boolean shift_level(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "shift_level")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = additive_level(b, l + 1);
    r = r && shift_level_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // shift_tail*
  private static boolean shift_level_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "shift_level_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!shift_tail(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "shift_level_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // ('<<' | '>>') additive_level
  public static boolean shift_tail(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "shift_tail")) return false;
    if (!nextTokenIsFast(b, GTGT, LTLT)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, BINARY_EXPRESSION, "<shift tail>");
    r = shift_tail_0(b, l + 1);
    p = r; // pin = 1
    r = r && additive_level(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // '<<' | '>>'
  private static boolean shift_tail_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "shift_tail_0")) return false;
    boolean r;
    r = consumeTokenFast(b, LTLT);
    if (!r) r = consumeTokenFast(b, GTGT);
    return r;
  }

  /* ********************************************************** */
  // pass_statement
  //   | break_statement
  //   | continue_statement
  //   | return_statement
  //   | load_statement
  //   | assignment_statement
  //   | aug_assignment_statement
  //   | expression_statement
  static boolean simple_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "simple_statement")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<statement>");
    r = pass_statement(b, l + 1);
    if (!r) r = break_statement(b, l + 1);
    if (!r) r = continue_statement(b, l + 1);
    if (!r) r = return_statement(b, l + 1);
    if (!r) r = load_statement(b, l + 1);
    if (!r) r = assignment_statement(b, l + 1);
    if (!r) r = aug_assignment_statement(b, l + 1);
    if (!r) r = expression_statement(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // slice_first_item (',' slice_item)*
  public static boolean slice_content(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "slice_content")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _UPPER_, SLICE_EXPRESSION, "<slice content>");
    r = slice_first_item(b, l + 1);
    r = r && slice_content_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (',' slice_item)*
  private static boolean slice_content_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "slice_content_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!slice_content_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "slice_content_1", c)) break;
    }
    return true;
  }

  // ',' slice_item
  private static boolean slice_content_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "slice_content_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && slice_item(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // slice_lower (':' slice_upper (':' test?)?)?
  public static boolean slice_first_item(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "slice_first_item")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, SLICE_ITEM, "<slice first item>");
    r = slice_lower(b, l + 1);
    r = r && slice_first_item_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (':' slice_upper (':' test?)?)?
  private static boolean slice_first_item_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "slice_first_item_1")) return false;
    slice_first_item_1_0(b, l + 1);
    return true;
  }

  // ':' slice_upper (':' test?)?
  private static boolean slice_first_item_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "slice_first_item_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COLON);
    r = r && slice_upper(b, l + 1);
    r = r && slice_first_item_1_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (':' test?)?
  private static boolean slice_first_item_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "slice_first_item_1_0_2")) return false;
    slice_first_item_1_0_2_0(b, l + 1);
    return true;
  }

  // ':' test?
  private static boolean slice_first_item_1_0_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "slice_first_item_1_0_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COLON);
    r = r && slice_first_item_1_0_2_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // test?
  private static boolean slice_first_item_1_0_2_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "slice_first_item_1_0_2_0_1")) return false;
    test(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // test? (':' test? (':' test?)?)?
  public static boolean slice_item(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "slice_item")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, SLICE_ITEM, "<slice item>");
    r = slice_item_0(b, l + 1);
    r = r && slice_item_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // test?
  private static boolean slice_item_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "slice_item_0")) return false;
    test(b, l + 1);
    return true;
  }

  // (':' test? (':' test?)?)?
  private static boolean slice_item_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "slice_item_1")) return false;
    slice_item_1_0(b, l + 1);
    return true;
  }

  // ':' test? (':' test?)?
  private static boolean slice_item_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "slice_item_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COLON);
    r = r && slice_item_1_0_1(b, l + 1);
    r = r && slice_item_1_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // test?
  private static boolean slice_item_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "slice_item_1_0_1")) return false;
    test(b, l + 1);
    return true;
  }

  // (':' test?)?
  private static boolean slice_item_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "slice_item_1_0_2")) return false;
    slice_item_1_0_2_0(b, l + 1);
    return true;
  }

  // ':' test?
  private static boolean slice_item_1_0_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "slice_item_1_0_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COLON);
    r = r && slice_item_1_0_2_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // test?
  private static boolean slice_item_1_0_2_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "slice_item_1_0_2_0_1")) return false;
    test(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // test | empty_expression
  static boolean slice_lower(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "slice_lower")) return false;
    boolean r;
    r = test(b, l + 1);
    if (!r) r = empty_expression(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // empty_expression | test?
  static boolean slice_upper(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "slice_upper")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = empty_expression(b, l + 1);
    if (!r) r = slice_upper_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // test?
  private static boolean slice_upper_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "slice_upper_1")) return false;
    test(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // ('*' | '**') test
  public static boolean star_argument_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "star_argument_expression")) return false;
    if (!nextTokenIs(b, "<star argument expression>", EXP, MULT)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, STAR_ARGUMENT_EXPRESSION, "<star argument expression>");
    r = star_argument_expression_0(b, l + 1);
    p = r; // pin = 1
    r = r && test(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // '*' | '**'
  private static boolean star_argument_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "star_argument_expression_0")) return false;
    boolean r;
    r = consumeToken(b, MULT);
    if (!r) r = consumeToken(b, EXP);
    return r;
  }

  /* ********************************************************** */
  // '*' bitwise_or_level
  public static boolean star_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "star_expression")) return false;
    if (!nextTokenIs(b, MULT)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, STAR_EXPRESSION, null);
    r = consumeToken(b, MULT);
    p = r; // pin = 1
    r = r && bitwise_or_level(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // <<withoutBracketGroups>> file_item*
  static boolean starlark_file(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "starlark_file")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = withoutBracketGroups(b, l + 1);
    r = r && starlark_file_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // file_item*
  private static boolean starlark_file_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "starlark_file_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!file_item(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "starlark_file_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // if_statement | for_statement | function_statement | simple_statement
  static boolean statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statement")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<statement>");
    r = if_statement(b, l + 1);
    if (!r) r = for_statement(b, l + 1);
    if (!r) r = function_statement(b, l + 1);
    if (!r) r = simple_statement(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // <<statementEnd>>
  static boolean statement_end(PsiBuilder b, int l) {
    return statementEnd(b, l + 1);
  }

  /* ********************************************************** */
  // indented_statements | <<indentExpected>>
  public static boolean statement_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statement_list")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, STATEMENT_LIST, "<statement list>");
    r = indented_statements(b, l + 1);
    if (!r) r = indentExpected(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // STATEMENT_BREAK | statement | unexpected_block | <<unexpectedStatementToken>>
  static boolean statement_or_break(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statement_or_break")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<statement>");
    r = consumeToken(b, STATEMENT_BREAK);
    if (!r) r = statement(b, l + 1);
    if (!r) r = unexpected_block(b, l + 1);
    if (!r) r = unexpectedStatementToken(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // !(<<atLineStart>> | STATEMENT_BREAK | DEDENT | ';')
  static boolean statement_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statement_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !statement_recover_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // <<atLineStart>> | STATEMENT_BREAK | DEDENT | ';'
  private static boolean statement_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statement_recover_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = atLineStart(b, l + 1);
    if (!r) r = consumeToken(b, STATEMENT_BREAK);
    if (!r) r = consumeToken(b, DEDENT);
    if (!r) r = consumeToken(b, SEMICOLON);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // STRING | BYTES
  public static boolean string_literal_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "string_literal_expression")) return false;
    if (!nextTokenIs(b, "<string>", BYTES, STRING)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, STRING_LITERAL_EXPRESSION, "<string>");
    r = consumeToken(b, STRING);
    if (!r) r = consumeToken(b, BYTES);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // load_string
  public static boolean string_load_value(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "string_load_value")) return false;
    if (!nextTokenIs(b, STRING)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = load_string(b, l + 1);
    exit_section_(b, m, STRING_LOAD_VALUE, r);
    return r;
  }

  /* ********************************************************** */
  // <<subscriptionHasColon>> slice_content | expression
  static boolean subscription_content(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_content")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_);
    r = subscription_content_0(b, l + 1);
    if (!r) r = expression(b, l + 1);
    exit_section_(b, l, m, r, false, _StarlarkParser::subscription_recover);
    return r;
  }

  // <<subscriptionHasColon>> slice_content
  private static boolean subscription_content_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_content_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = subscriptionHasColon(b, l + 1);
    r = r && slice_content(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // ']'
  static boolean subscription_end(PsiBuilder b, int l) {
    return consumeToken(b, RBRACKET);
  }

  /* ********************************************************** */
  // '[' <<plainMode subscription_content>> subscription_end
  public static boolean subscription_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_expression")) return false;
    if (!nextTokenIsFast(b, LBRACKET)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, SUBSCRIPTION_EXPRESSION, null);
    r = consumeTokenFast(b, LBRACKET);
    p = r; // pin = 1
    r = r && report_error_(b, plainMode(b, l + 1, _StarlarkParser::subscription_content));
    r = p && subscription_end(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // !(']' | header_colon | STATEMENT_BREAK)
  static boolean subscription_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !subscription_recover_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ']' | header_colon | STATEMENT_BREAK
  private static boolean subscription_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subscription_recover_0")) return false;
    boolean r;
    r = consumeToken(b, RBRACKET);
    if (!r) r = header_colon(b, l + 1);
    if (!r) r = consumeToken(b, STATEMENT_BREAK);
    return r;
  }

  /* ********************************************************** */
  // block_suite | inline_statement_list
  static boolean suite(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "suite")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<statement>");
    r = block_suite(b, l + 1);
    if (!r) r = inline_statement_list(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // lambda_expression | conditional_level
  static boolean test(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "test")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<expression>");
    r = lambda_expression(b, l + 1);
    if (!r) r = conditional_level(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // 'True'
  public static boolean true_literal_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "true_literal_expression")) return false;
    if (!nextTokenIs(b, TRUE_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, TRUE_KEYWORD);
    exit_section_(b, m, TRUE_LITERAL_EXPRESSION, r);
    return r;
  }

  /* ********************************************************** */
  // ',' (test (',' test)* ','?)?
  public static boolean tuple_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tuple_expression")) return false;
    if (!nextTokenIs(b, COMMA)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _LEFT_, TUPLE_EXPRESSION, null);
    r = consumeToken(b, COMMA);
    r = r && tuple_expression_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (test (',' test)* ','?)?
  private static boolean tuple_expression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tuple_expression_1")) return false;
    tuple_expression_1_0(b, l + 1);
    return true;
  }

  // test (',' test)* ','?
  private static boolean tuple_expression_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tuple_expression_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = test(b, l + 1);
    r = r && tuple_expression_1_0_1(b, l + 1);
    r = r && tuple_expression_1_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (',' test)*
  private static boolean tuple_expression_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tuple_expression_1_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!tuple_expression_1_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "tuple_expression_1_0_1", c)) break;
    }
    return true;
  }

  // ',' test
  private static boolean tuple_expression_1_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tuple_expression_1_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && test(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ','?
  private static boolean tuple_expression_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tuple_expression_1_0_2")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // prefix_expression | power_level
  static boolean unary_level(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unary_level")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<expression>");
    r = prefix_expression(b, l + 1);
    if (!r) r = power_level(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // <<unexpectedIndent>> block_item* (DEDENT | <<eof>>)
  static boolean unexpected_block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unexpected_block")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = unexpectedIndent(b, l + 1);
    p = r; // pin = 1
    r = r && report_error_(b, unexpected_block_1(b, l + 1));
    r = p && unexpected_block_2(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // block_item*
  private static boolean unexpected_block_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unexpected_block_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!block_item(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "unexpected_block_1", c)) break;
    }
    return true;
  }

  // DEDENT | <<eof>>
  private static boolean unexpected_block_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unexpected_block_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, DEDENT);
    if (!r) r = eof(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '*' IDENTIFIER
  public static boolean variadic_parameter(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "variadic_parameter")) return false;
    if (!nextTokenIs(b, MULT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, MULT, IDENTIFIER);
    exit_section_(b, m, VARIADIC_PARAMETER, r);
    return r;
  }

  static final Parser COLON_parser_ = (b, l) -> consumeToken(b, COLON);
  static final Parser RBRACE_parser_ = (b, l) -> consumeToken(b, RBRACE);
  static final Parser RBRACKET_parser_ = (b, l) -> consumeToken(b, RBRACKET);
  static final Parser RPAR_parser_ = (b, l) -> consumeToken(b, RPAR);
}
