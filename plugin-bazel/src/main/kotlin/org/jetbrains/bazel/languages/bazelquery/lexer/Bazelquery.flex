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
SOFT_NL=\\\r?\n
SPACE=[\ \t]

SQ=[']
DQ=[\"]
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

UNEXPECTED_WORD=[^-{NL}{SPACE}][^{NL}{SPACE}]*
FLAG_WORD=[a-z_:]*

INTEGER=[0-9]+

OPTION={HYP}{HYP}{FLAG_WORD} | {HYP}{FLAG_WORD}

// TODO: check if it is possible to generate this list dynamicly and change lexer on updates
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

// lexing states:
%xstate EXPR
%xstate WORD_DQ, WORD_SQ

%xstate FLAG
%xstate PRE_VALUE
%xstate VALUE, VALUE_SQ, VALUE_DQ
%xstate SPACE_NEEDED

%%

// initial state, which starts expression or flag
<YYINITIAL> {
    ({SOFT_NL} | [{SPACE}{NL}])+      { return TokenType.WHITE_SPACE; }

    {HYP}                             { yybegin(FLAG);  yypushback(1); }
    ([A-Za-z0-9/@._:$~\[\]] | {SQ} | {DQ})           { yybegin(EXPR); yypushback(1); }
    [^]                               { yybegin(FLAG);  yypushback(1); }
}

// _____EXPRESSIONS_____

// states to tokenize the word in quotes inside the expression
<WORD_SQ> {
     {SQ}{SQ}                         { yybegin(EXPR); return BazelqueryTokenTypes.SQ_EMPTY; }
     {SQ_WORD}                        { yybegin(EXPR); return BazelqueryTokenTypes.SQ_WORD; }
     {SQ}{QUOTED_WORD}                { return BazelqueryTokenTypes.SQ_UNFINISHED; }
     {SQ}                             { return BazelqueryTokenTypes.SQ_UNFINISHED; }
     [^]                              { yybegin(EXPR); yypushback(1); }
 }

<WORD_DQ> {
      {DQ}{DQ}                        { yybegin(EXPR); return BazelqueryTokenTypes.DQ_EMPTY; }
      {DQ_WORD}                       { yybegin(EXPR); return BazelqueryTokenTypes.DQ_WORD; }
      {DQ}{QUOTED_WORD}               { return BazelqueryTokenTypes.DQ_UNFINISHED; }
      {DQ}                            { return BazelqueryTokenTypes.DQ_UNFINISHED; }
      [^]                             { yybegin(EXPR); yypushback(1); }
  }

// expression tokenization
<EXPR> {
    ({SOFT_NL} | [{SPACE}{NL}])+      { return TokenType.WHITE_SPACE; }

    // operations
    {PLUS} | "union"                  { return BazelqueryTokenTypes.UNION; }
    {HYPHEN} | "except"               { return BazelqueryTokenTypes.EXCEPT; }
    {CARET} | "intersect"             { return BazelqueryTokenTypes.INTERSECT; }
    "let"                             { return BazelqueryTokenTypes.LET; }
    "in"                              { return BazelqueryTokenTypes.IN; }
    "="                               { return BazelqueryTokenTypes.EQUALS; }
    "set"                             { return BazelqueryTokenTypes.SET; }

    // commands (=functions)
    "allpaths"                        { return BazelqueryTokenTypes.ALLPATHS; }
    "attr"                            { return BazelqueryTokenTypes.ATTR; }
    "buildfiles"                      { return BazelqueryTokenTypes.BUILDFILES; }
    "rbuildfiles"                     { return BazelqueryTokenTypes.RBUILDFILES; }
    "deps"                            { return BazelqueryTokenTypes.DEPS; }
    "filter"                          { return BazelqueryTokenTypes.FILTER; }
    "kind"                            { return BazelqueryTokenTypes.KIND; }
    "labels"                          { return BazelqueryTokenTypes.LABELS; }
    "loadfiles"                       { return BazelqueryTokenTypes.LOADFILES; }
    "rdeps"                           { return BazelqueryTokenTypes.RDEPS; }
    "allrdeps"                        { return BazelqueryTokenTypes.ALLRDEPS; }
    "same_pkg_direct_rdeps"           { return BazelqueryTokenTypes.SAME_PKG_DIRECT_RDEPS; }
    "siblings"                        { return BazelqueryTokenTypes.SIBLINGS; }
    "some"                            { return BazelqueryTokenTypes.SOME; }
    "somepath"                        { return BazelqueryTokenTypes.SOMEPATH; }
    "tests"                           { return BazelqueryTokenTypes.TESTS; }
    "visible"                         { return BazelqueryTokenTypes.VISIBLE; }


    {LEFT_PAREN}                      { return BazelqueryTokenTypes.LEFT_PAREN; }
    {RIGHT_PAREN}                     { return BazelqueryTokenTypes.RIGHT_PAREN; }
    ","                               { return BazelqueryTokenTypes.COMMA; }

    {INTEGER}                         { return BazelqueryTokenTypes.INTEGER; }
    {DQ}                              { yybegin(WORD_DQ); yypushback(1); }
    {SQ}                              { yybegin(WORD_SQ); yypushback(1); }
    {UNQUOTED_WORD}                   { return BazelqueryTokenTypes.UNQUOTED_WORD; }

    [^]                               { yybegin(YYINITIAL); yypushback(1); }
}


// _____FLAGS_____

// expecting space at the end of option (with or without value)
<SPACE_NEEDED> {
    {SPACE} | {NL}                    { yybegin(YYINITIAL); return BazelqueryTokenTypes.WHITE_SPACE; }
    [^]                               { yybegin(YYINITIAL); yypushback(1); return BazelqueryTokenTypes.MISSING_SPACE; }
}

// option name
<FLAG> {
    {HYP} | {HYP}{HYP}                { return BazelqueryTokenTypes.UNFINISHED_FLAG; }
    {OPTION_REQ_VALUE}{SPACE}         { yybegin(PRE_VALUE); yypushback(1); return BazelqueryTokenTypes.FLAG; }
    {OPTION_REQ_VALUE}{EQALS}         { yybegin(PRE_VALUE); yypushback(1); return BazelqueryTokenTypes.FLAG; }
    {OPTION_REQ_VALUE}                { yybegin(PRE_VALUE); return BazelqueryTokenTypes.FLAG; }
    {OPTION}{EQALS}                   { yybegin(PRE_VALUE); yypushback(1); return BazelqueryTokenTypes.FLAG; }
    {OPTION}                          { yybegin(SPACE_NEEDED); return BazelqueryTokenTypes.FLAG_NO_VAL; }

    {OPTION_REQ_VALUE}[^{EQALS}{SPACE}][-]*{UNEXPECTED_WORD}*     { yybegin(YYINITIAL); return BazelqueryTokenTypes.UNEXPECTED; }
    {UNEXPECTED_WORD}                 { yybegin(YYINITIAL); return BazelqueryTokenTypes.UNEXPECTED; }
}

// expecting space or equals sign if option requires value
<PRE_VALUE> {
    {SPACE} | {EQALS}                 { yybegin(VALUE); return BazelqueryTokenTypes.EQUALS; }
}


// value of the option (can be also quoted)

<VALUE> {
    ({SOFT_NL} | [{SPACE}{NL}])+      { yybegin(YYINITIAL); return TokenType.WHITE_SPACE; }
    {SQ}                              { yybegin(VALUE_SQ); yypushback(1); }
    {DQ}                              { yybegin(VALUE_DQ); yypushback(1); }

    {UNQUOTED_WORD}                   { yybegin(SPACE_NEEDED); return BazelqueryTokenTypes.UNQUOTED_VAL; }

    [^]                               { yybegin(YYINITIAL); yypushback(1); }
}

<VALUE_SQ> {
    {SQ}{SQ}                          { yybegin(SPACE_NEEDED); return BazelqueryTokenTypes.SQ_VAL; }
    {SQ}({QUOTED_WORD} | {DQ})+{SQ}   { yybegin(SPACE_NEEDED); return BazelqueryTokenTypes.SQ_VAL; }
    {SQ}({QUOTED_WORD} | {DQ})+       { yybegin(SPACE_NEEDED); return BazelqueryTokenTypes.UNFINISHED_VAL; }
    {SQ}                              { yybegin(SPACE_NEEDED); return BazelqueryTokenTypes.UNFINISHED_VAL; }
    [^]                               { yybegin(YYINITIAL); yypushback(1); }
}

<VALUE_DQ> {
    {DQ}{DQ}                          { yybegin(SPACE_NEEDED); return BazelqueryTokenTypes.DQ_VAL; }
    {DQ}({QUOTED_WORD} | {SQ})+{DQ}   { yybegin(SPACE_NEEDED); return BazelqueryTokenTypes.DQ_VAL; }
    {DQ}({QUOTED_WORD} | {SQ})+       { yybegin(SPACE_NEEDED); return BazelqueryTokenTypes.UNFINISHED_VAL; }
    {DQ}                              { yybegin(SPACE_NEEDED); return BazelqueryTokenTypes.UNFINISHED_VAL; }
    [^]                               { yybegin(YYINITIAL); yypushback(1); }
}
