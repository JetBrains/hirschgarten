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
DHYP=[\-\-]
HYP=[\-]

PLUS=[+]
HYPHEN=[-]
CARET=[\^]
EQALS=[=]

LEFT_PAREN=[(]
RIGHT_PAREN=[)]
DOUBLE_AT=[@@]


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
UNQUOTED_WORD=([A-Za-z0-9/@._:$~\[\]][A-Za-z0-9*/@.\-_:$~\[\]]* | {DOUBLE_AT}[A-Za-z0-9*/@.\-_:$~\[\]+]*)
QUOTED_WORD=[^{NL}{SQ}{DQ}]+

SQ_WORD={SQ}{QUOTED_WORD}{SQ}
DQ_WORD={DQ}{QUOTED_WORD}{DQ}

UNEXPECTED_WORD=[^-{NL}{SQ}{DQ}{SPACE}][^{NL}{SQ}{DQ}{SPACE}]*
FLAG_WORD=[a-z_:]*

OPTION={HYP}{HYP}{FLAG_WORD} | {HYP}{FLAG_WORD}

OPTION_REQ_VALUE="--loading_phase_threads" |
                 "--aspect_deps" |
                 "--graph:conditional_edges_limit" |
                 "--graph:node_limit" |
                 "--order_output" |
                 "--output" |
                 "--output_file" |
                 "--proto:output_rule_attrs" |
                 "--query_file" |
                 "--universe_scope" |
                 "--experimental_repository_resolved_file" |
                 "--deleted_packages" |
                 "--package_path"
//INTEGER=[0-9]+

COMMENT=#({SOFT_NL} | [^\r\n])*     // potrzebne????
COMMAND="allpaths" | "attr" | "buildfiles" | "rbuildfiles" | "deps" | "filter" | "kind" | "labels" | "loadfiles" | "rdeps" | "allrdeps" | "same_pkg_direct_rdeps" | "siblings" | "some" | "somepath" | "tests" | "visible"

// lexing states:
%xstate EXPR
%xstate EXPR_DQ
%xstate EXPR_SQ

%xstate WORD_DQ
%xstate WORD_SQ


%xstate FLAG, VALUE, PRE_VALUE


%%


<YYINITIAL> {
    ({SOFT_NL} | [{SPACE}{NL}])+      { return TokenType.WHITE_SPACE; }
    {COMMENT}+                        { return BazelqueryTokenTypes.COMMENT; }

    "bazel"                           { return BazelqueryTokenTypes.BAZEL; }
    "query"                           { return BazelqueryTokenTypes.QUERY; }

    {HYP}                             { yybegin(FLAG);  yypushback(1); }

    {SQ}                              { yybegin(EXPR_SQ); return BazelqueryTokenTypes.SINGLE_QUOTE; }
    {DQ}                              { yybegin(EXPR_DQ); return BazelqueryTokenTypes.DOUBLE_QUOTE; }
    [A-Za-z0-9/@._:$~\[\]]            { yybegin(EXPR); yypushback(1); }
    [^]                               { yybegin(FLAG);  yypushback(1); }
}

<EXPR_DQ> {
    ({SOFT_NL} | [{SPACE}{NL}])+      { return TokenType.WHITE_SPACE; }
    {DQ}                              { yybegin(YYINITIAL); return BazelqueryTokenTypes.DOUBLE_QUOTE; }
    //{SQ}                              { yybegin(WORD_SQ); yypushback(1); return BazelqueryTokenTypes.SINGLE_QUOTE; }
    // {SQ} ([{QUOTED_WORD}{ESQ}{EDQ}])+ {SQ}       { return BazelqueryTokenTypes.SQ_WORD; }
    {SQ_WORD}                         { return BazelqueryTokenTypes.SQ_WORD; }
   //  [^]                              {}
}

<EXPR_SQ> {
    ({SOFT_NL} | [{SPACE}{NL}])+      { return TokenType.WHITE_SPACE; }
    {SQ}                              { yybegin(YYINITIAL); return BazelqueryTokenTypes.SINGLE_QUOTE; }
    //{DQ}                              { yybegin(WORD_DQ); yypushback(1); return BazelqueryTokenTypes.DOUBLE_QUOTE; }
    // {DQ} ({QUOTED_WORD} | {ESQ} | {EDQ})+ {DQ}        { return BazelqueryTokenTypes.DQ_WORD; }
    {DQ_WORD}                         { return BazelqueryTokenTypes.DQ_WORD; }
 //   [^]                                {}
}

<EXPR> {
    ({SOFT_NL} | [{SPACE}{NL}])      { yybegin(YYINITIAL); return BazelqueryTokenTypes.WHITE_SPACE; }
}

<EXPR, EXPR_DQ, EXPR_SQ> {
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

    {UNQUOTED_WORD}                   { return BazelqueryTokenTypes.UNQUOTED_WORD; }
   // [^]
}

<FLAG> {
    {OPTION_REQ_VALUE}{SPACE} { yybegin(PRE_VALUE); yypushback(1); return BazelqueryTokenTypes.FLAG; }
    {OPTION_REQ_VALUE}{EQALS} { yybegin(PRE_VALUE); yypushback(1); return BazelqueryTokenTypes.FLAG; }
    {OPTION_REQ_VALUE}        { yybegin(PRE_VALUE); return BazelqueryTokenTypes.FLAG; }
    {OPTION}{EQALS}           { yybegin(PRE_VALUE); yypushback(1); return BazelqueryTokenTypes.FLAG; }
    {OPTION}                  { yybegin(YYINITIAL); return BazelqueryTokenTypes.FLAG_NO_VAL; }

    {UNEXPECTED_WORD}         { yybegin(YYINITIAL); return BazelqueryTokenTypes.UNEXPECTED; }
}

<PRE_VALUE> {
    {SPACE} | {EQALS}         { yybegin(VALUE); return BazelqueryTokenTypes.EQUALS; }
}

<VALUE> {
    ({SOFT_NL} | [{SPACE}{NL}])+     { yybegin(YYINITIAL); return TokenType.WHITE_SPACE; }
    {SQ_WORD}                 { yybegin(YYINITIAL); return BazelqueryTokenTypes.SQ_VAL; }
    {DQ_WORD}                 { yybegin(YYINITIAL); return BazelqueryTokenTypes.DQ_VAL; }
    {UNQUOTED_WORD}           { yybegin(YYINITIAL); return BazelqueryTokenTypes.UNQUOTED_VAL; }

    [^]                       { yybegin(YYINITIAL); yypushback(1); } // return UNEXPECTED???
}


