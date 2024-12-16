package org.jetbrains.bazel.languages.bazelquery.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenTypes;

@SuppressWarnings("UnnecessaryUnicodeEscape")
%%
%class _BazelqueryLexer
%implements FlexLexer
%no_suppress_warnings
%unicode
%function advance
%type IElementType

NL=[\r\n]
SOFT_NL=\\\r?\n              // potrzebne????
SPACE=[\ \t]

SQ=[']
DQ=[\"]
DH=[\-\-]

PLUS=[+]
HYPHEN=[-]
CARET=[\^]

LEFT_PAREN=[(]
RIGHT_PAREN=[)]


// Unquoted words are sequences of characters drawn from
// the alphabet characters A-Za-z,
// the numerals 0-9,
// and the special characters */@.-_:$~[]
// (asterisk, forward slash, at, period, hyphen, underscore, colon,
// dollar sign, tilde, left square brace, right square brace).
// However, unquoted words may not start with a hyphen - or asterisk *
// even though relative target names may start with those characters.
// As a special rule meant to simplify the handling of labels referring
// to external repositories, unquoted words that start with @@ may
// contain + characters.
UNQUOTED_WORD=([A-Za-z0-9/@._:$~\[\]][A-Za-z0-9*/@.\-_:$~\[\]]* | @@[A-Za-z0-9*/@.\-_:$~\[\]+]*)

QUOTED_WORD=[^{NL}]+


ESQ=\\{SQ}
EDQ=\\{DQ}
//SQ_WORD={SQ}{QUOTED_WORD}{SQ}
//DQ_WORD={DQ}{QUOTED_WORD}{DQ}

//INTEGER=[0-9]+

COMMENT=#({SOFT_NL} | [^\r\n])*     // potrzebne????
COMMAND="allpaths" | "attr" | "buildfiles" | "rbuildfiles" | "deps" | "filter" | "kind" | "labels" | "loadfiles" | "rdeps" | "allrdeps" | "same_pkg_direct_rdeps" | "siblings" | "some" | "somepath" | "tests" | "visible"

// lexing states:
%xstate EXPR
%xstate EXPR_DQ
%xstate EXPR_SQ

%xstate WORD_DQ
%xstate WORD_SQ


%xstate FLAG, VALUE
%xstate FLAG_DQ, VALUE_DQ
%xstate FLAG_SQ, VALUE_SQ


%%


<YYINITIAL> {
    ({SOFT_NL} | [{SPACE}{NL}])+      { return TokenType.WHITE_SPACE; }
    {COMMENT}+                        { return BazelqueryTokenTypes.COMMENT; }

    "bazel"                           { return BazelqueryTokenTypes.BAZEL; }
    "query"                           { return BazelqueryTokenTypes.QUERY; }

    {DH}                              { /*yybegin(FLAG);*/ return BazelqueryTokenTypes.DOUBLE_HYPHEN; }

    {SQ}                              { yybegin(EXPR_SQ); return BazelqueryTokenTypes.SINGLE_QUOTE; }
    {DQ}                              { yybegin(EXPR_DQ); return BazelqueryTokenTypes.DOUBLE_QUOTE; }
    [^]                               { yybegin(EXPR); yypushback(1); }
}

<EXPR_DQ> {
    ({SOFT_NL} | [{SPACE}{NL}])+      { return TokenType.WHITE_SPACE; }
    {DQ}                              { yybegin(YYINITIAL); return BazelqueryTokenTypes.DOUBLE_QUOTE; }
    {SQ}                              { yybegin(WORD_SQ); return BazelqueryTokenTypes.SINGLE_QUOTE; }
}

<EXPR_SQ> {
    ({SOFT_NL} | [{SPACE}{NL}])+      { return TokenType.WHITE_SPACE; }
    {SQ}                              { yybegin(YYINITIAL); return BazelqueryTokenTypes.SINGLE_QUOTE; }
    {DQ}                              { yybegin(WORD_DQ); return BazelqueryTokenTypes.DOUBLE_QUOTE; }
}

<EXPR> {
    ({SOFT_NL} | [{SPACE}{NL}])+      { yybegin(YYINITIAL); return TokenType.WHITE_SPACE; }
}

<EXPR_DQ, EXPR_SQ, EXPR> {
    {LEFT_PAREN}                      { return BazelqueryTokenTypes.LEFT_PAREN; }
    {RIGHT_PAREN}                     { return BazelqueryTokenTypes.RIGHT_PAREN; }

    {PLUS} | "union"                  { return BazelqueryTokenTypes.UNION; }
    {HYPHEN} | "except"               { return BazelqueryTokenTypes.EXCEPT; }
    {CARET} | "intersect"             { return BazelqueryTokenTypes.INTERSECT; }
    "let"                             { return BazelqueryTokenTypes.LET; }
    "in"                              { return BazelqueryTokenTypes.IN; }
    "="                               { return BazelqueryTokenTypes.EQUALS; }
    "set"                             { return BazelqueryTokenTypes.SET; }

    {COMMAND}                         { return BazelqueryTokenTypes.COMMAND; }

    ","                               { return BazelqueryTokenTypes.COMMA; }

    {UNQUOTED_WORD}                   { return BazelqueryTokenTypes.WORD; }
   // [^]
}

<WORD_SQ> {
 //   ([{QUOTED_WORD}{ESQ}{EDQ}])+        { return BazelqueryTokenTypes.WORD; }
    {SQ}                              { yybegin(EXPR_DQ); return BazelqueryTokenTypes.SINGLE_QUOTE; }
}

<WORD_DQ> {
 //   ({QUOTED_WORD} | {ESQ} | {EDQ})+        { return BazelqueryTokenTypes.WORD; }
    {DQ}                              { yybegin(EXPR_SQ); return BazelqueryTokenTypes.DOUBLE_QUOTE; }
}
/*
<FLAG> {
    {UNQUOTED_WORD}                   { yybegin(VALUE); return BazelqueryTokenTypes.FLAG; }
    {SQ}                              { yybegin(FLAG_SQ); return BazelqueryTokenTypes.SINGLE_QUOTE; }
    {DQ}                              { yybegin(FLAG_DQ); return BazelqueryTokenTypes.DOUBLE_QUOTE; }
}
*/


