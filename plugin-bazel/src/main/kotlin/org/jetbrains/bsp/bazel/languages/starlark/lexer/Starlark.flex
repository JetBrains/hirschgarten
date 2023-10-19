package org.jetbrains.bsp.bazel.languages.starlark.lexer;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.bsp.bazel.languages.starlark.elements.StarlarkTokenTypes;

%%

%class _StarlarkLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType

INT = {DECIMAL_LIT} | {OCTAL_LIT} | {HEX_LIT} | 0
DECIMAL_LIT = [1-9]{DECIMAL_DIGIT}*
OCTAL_LIT = 0[oO]{OCTAL_DIGIT}+
HEX_LIT = 0[xX]{HEX_DIGIT}+

FLOAT = ({DECIMALS}\.{DECIMALS}?{EXPONENT}?) | ({DECIMALS}{EXPONENT}) | (\.{DECIMALS}{EXPONENT}?)
DECIMALS = {DECIMAL_DIGIT}+
EXPONENT = [eE][+-]?{DECIMALS}

DECIMAL_DIGIT = [0-9]
OCTAL_DIGIT = [0-7]
HEX_DIGIT = [0-9A-Fa-f]

COMMENT="#"[^\r\n]*

IDENTIFIER = [^\d\W]\w*

STRING = r?{STRING_CORE}
BYTES = (b|br|rb){STRING_CORE}
STRING_CORE = {APOSTROPHE_STRING} | {QUOTE_STRING} | {TRIPLE_APOSTROPHE_STRING} | {TRIPLE_QUOTE_STRING}

APOSTROPHE_STRING = \'{APOSTROPHE_STRING_ITEM}*\'
APOSTROPHE_STRING_ITEM = {APOSTROPHE_STRING_CHAR} | {ESCAPE_SEQUENCE}
APOSTROPHE_STRING_CHAR = [^\\\n\']

QUOTE_STRING = \"{QUOTE_STRING_ITEM}*\"
QUOTE_STRING_ITEM = {QUOTE_STRING_CHAR} | {ESCAPE_SEQUENCE}
QUOTE_STRING_CHAR = [^\\\n\"]

TRIPLE_APOSTROPHE_STRING = (\'\'\'{TRIPLE_APOSTROPHE_STRING_ITEM}*\'\'\')
TRIPLE_APOSTROPHE_STRING_ITEM = [^\\'] | {ONE_TWO_APOSTROPHES} | {ESCAPE_SEQUENCE}
ONE_TWO_APOSTROPHES = ('[^\\']) | ('\\[^]) | (''[^\\']) | (''\\[^])

TRIPLE_QUOTE_STRING = (\"\"\"{TRIPLE_QUOTE_STRING_ITEM}*\"\"\")
TRIPLE_QUOTE_STRING_ITEM = [^\\\"] | {ONE_TWO_QUOTES} | {ESCAPE_SEQUENCE}
ONE_TWO_QUOTES = (\"[^\\\"]) | (\"\\[^]) | (\"\"[^\\\"]) | (\"\"\\[^])

LINE_CONTINUATION = \\[\n]
ESCAPE_SEQUENCE = \\[^]
%%

[\ ]          { return StarlarkTokenTypes.SPACE; }
[\t]          { return StarlarkTokenTypes.TAB; }
[\n]          { return StarlarkTokenTypes.LINE_BREAK; }
{LINE_CONTINUATION} { return StarlarkTokenTypes.LINE_CONTINUATION; }

{COMMENT}     { return StarlarkTokenTypes.COMMENT; }

{STRING}      { return StarlarkTokenTypes.STRING; }
{BYTES}       { return StarlarkTokenTypes.BYTES; }
{INT}         { return StarlarkTokenTypes.INT; }
{FLOAT}       { return StarlarkTokenTypes.FLOAT; }


"and"         { return StarlarkTokenTypes.AND_KEYWORD; }
"break"       { return StarlarkTokenTypes.BREAK_KEYWORD; }
"continue"    { return StarlarkTokenTypes.CONTINUE_KEYWORD; }
"def"         { return StarlarkTokenTypes.DEF_KEYWORD; }
"elif"        { return StarlarkTokenTypes.ELIF_KEYWORD; }
"else"        { return StarlarkTokenTypes.ELSE_KEYWORD; }
"for"         { return StarlarkTokenTypes.FOR_KEYWORD; }
"if"          { return StarlarkTokenTypes.IF_KEYWORD; }
"in"          { return StarlarkTokenTypes.IN_KEYWORD; }
"lambda"      { return StarlarkTokenTypes.LAMBDA_KEYWORD; }
"load"        { return StarlarkTokenTypes.LOAD_KEYWORD; }
"not"         { return StarlarkTokenTypes.NOT_KEYWORD; }
"or"          { return StarlarkTokenTypes.OR_KEYWORD; }
"pass"        { return StarlarkTokenTypes.PASS_KEYWORD; }
"return"      { return StarlarkTokenTypes.RETURN_KEYWORD; }

"as"          { return StarlarkTokenTypes.AS_KEYWORD; }
"assert"      { return StarlarkTokenTypes.ASSERT_KEYWORD; }
"async"       { return StarlarkTokenTypes.ASYNC_KEYWORD; }
"await"       { return StarlarkTokenTypes.AWAIT_KEYWORD; }
"class"       { return StarlarkTokenTypes.CLASS_KEYWORD; }
"del"         { return StarlarkTokenTypes.DEL_KEYWORD; }
"except"      { return StarlarkTokenTypes.EXCEPT_KEYWORD; }
"finally"     { return StarlarkTokenTypes.FINALLY_KEYWORD; }
"from"        { return StarlarkTokenTypes.FROM_KEYWORD; }
"global"      { return StarlarkTokenTypes.GLOBAL_KEYWORD; }
"import"      { return StarlarkTokenTypes.IMPORT_KEYWORD; }
"is"          { return StarlarkTokenTypes.IS_KEYWORD; }
"nonlocal"    { return StarlarkTokenTypes.NONLOCAL_KEYWORD; }
"raise"       { return StarlarkTokenTypes.RAISE_KEYWORD; }
"try"         { return StarlarkTokenTypes.TRY_KEYWORD; }
"while"       { return StarlarkTokenTypes.WHILE_KEYWORD; }
"with"        { return StarlarkTokenTypes.WITH_KEYWORD; }
"yield"       { return StarlarkTokenTypes.YIELD_KEYWORD; }

{IDENTIFIER}  { return StarlarkTokenTypes.IDENTIFIER; }

"+"           { return StarlarkTokenTypes.PLUS; }
"-"           { return StarlarkTokenTypes.MINUS; }
"*"           { return StarlarkTokenTypes.MULT; }
"/"           { return StarlarkTokenTypes.DIV; }
"//"          { return StarlarkTokenTypes.FLOORDIV; }
"%"           { return StarlarkTokenTypes.PERC; }
"**"          { return StarlarkTokenTypes.EXP; }
"~"           { return StarlarkTokenTypes.TILDE; }
"&"           { return StarlarkTokenTypes.AND; }
"|"           { return StarlarkTokenTypes.OR; }
"^"           { return StarlarkTokenTypes.XOR; }
"<<"          { return StarlarkTokenTypes.LTLT; }
">>"          { return StarlarkTokenTypes.GTGT; }
"."           { return StarlarkTokenTypes.DOT; }
","           { return StarlarkTokenTypes.COMMA; }
"="           { return StarlarkTokenTypes.EQ; }
";"           { return StarlarkTokenTypes.SEMICOLON; }
":"           { return StarlarkTokenTypes.COLON; }
"("           { return StarlarkTokenTypes.LPAR; }
")"           { return StarlarkTokenTypes.RPAR; }
"["           { return StarlarkTokenTypes.LBRACKET; }
"]"           { return StarlarkTokenTypes.RBRACKET; }
"{"           { return StarlarkTokenTypes.LBRACE; }
"}"           { return StarlarkTokenTypes.RBRACE; }
"<"           { return StarlarkTokenTypes.LT; }
">"           { return StarlarkTokenTypes.GT; }
">="          { return StarlarkTokenTypes.GE; }
"<="          { return StarlarkTokenTypes.LE; }
"=="          { return StarlarkTokenTypes.EQEQ; }
"!="          { return StarlarkTokenTypes.NE; }
"+="          { return StarlarkTokenTypes.PLUSEQ; }
"-="          { return StarlarkTokenTypes.MINUSEQ; }
"*="          { return StarlarkTokenTypes.MULTEQ; }
"/="          { return StarlarkTokenTypes.DIVEQ; }
"//="         { return StarlarkTokenTypes.FLOORDIVEQ; }
"%="          { return StarlarkTokenTypes.PERCEQ; }
"&="          { return StarlarkTokenTypes.ANDEQ; }
"|="          { return StarlarkTokenTypes.OREQ; }
"^="          { return StarlarkTokenTypes.XOREQ; }
"<<="         { return StarlarkTokenTypes.LTLTEQ; }
">>="         { return StarlarkTokenTypes.GTGTEQ; }


[^]           { return TokenType.BAD_CHARACTER; }