package org.jetbrains.bsp.bazel.languages.starlark;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static org.jetbrains.bsp.bazel.languages.starlark.StarlarkTypes.*;

%%

%{
  public StarlarkLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class StarlarkLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

EOL=\R
WHITE_SPACE=\s+

INT=([1-9][0-9]*)|(0(o|O)[0-7]+)|(0(x|X)[a-fA-F0-9]+)|0
FLOAT=([0-9]+\.[0-9]*((e|E)(\+|\-)?[0-9]+)?)|([0-9]+(e|E)(\+|\-)?[0-9]+)|(\.[0-9]*((e|E)(\+|\-)?[0-9]+)?)
IDENTIFIER=[a-zA-Z_][a-zA-Z0-9_]*
STRING=r?(('''((([^']|\n)*)|(([^']|\n)*'([^']|\n)+)|(([^']|\n)*''([^']|\n)+))*''')|(\"\"\"((([^\"]|\n)*)|(([^\"]|\n)*\"([^\"]|\n)+)|(([^\"]|\n)*\"\"([^\"]|\n)+))*\"\"\")|('([^']|(\\\n))*')|(\"([^\"]|(\\\n))*\"))
BYTES=(b|rb|br)(('''((([^']|\n)*)|(([^']|\n)*'([^']|\n)+)|(([^']|\n)*''([^']|\n)+))*''')|(\"\"\"((([^\"]|\n)*)|(([^\"]|\n)*\"([^\"]|\n)+)|(([^\"]|\n)*\"\"([^\"]|\n)+))*\"\"\")|('([^']|(\\\n))*')|(\"([^\"]|(\\\n))*\"))
COMMENT=#.*
WHITE_SPACE=([\s]|\\\n)+

%%
<YYINITIAL> {
  {WHITE_SPACE}      { return WHITE_SPACE; }

  "def"              { return DEF; }
  "lambda"           { return LAMBDA; }
  "if"               { return IF; }
  "elif"             { return ELIF; }
  "else"             { return ELSE; }
  "for"              { return FOR; }
  "in"               { return IN; }
  "return"           { return RETURN; }
  "break"            { return BREAK; }
  "continue"         { return CONTINUE; }
  "pass"             { return PASS; }
  "load"             { return LOAD; }
  "="                { return EQ; }
  "+="               { return PLUS_EQ; }
  "-="               { return MINUS_EQ; }
  "*="               { return ASTERISK_EQ; }
  "/="               { return DIV_EQ; }
  "//="              { return DIV_INT_EQ; }
  "%="               { return MODULO_EQ; }
  "&="               { return AND_EQ; }
  "|="               { return OR_EQ; }
  "^="               { return XOR_EQ; }
  "<<="              { return SHIFT_LEFT_EQ; }
  ">>="              { return SHIFT_RIGHT_EQ; }
  "+"                { return PLUS; }
  "-"                { return MINUS; }
  "*"                { return ASTERISK; }
  "%"                { return MODULO; }
  "/"                { return DIV; }
  "//"               { return DIV_INT; }
  "or"               { return OR; }
  "and"              { return AND; }
  "not"              { return NOT; }
  "=="               { return IS_EQUAL; }
  "!="               { return IS_NOT_EQUAL; }
  "<"                { return LESS_THAN; }
  ">"                { return GREATER_THAN; }
  "<="               { return LESS_OR_EQUAL; }
  ">="               { return GREATER_OR_EQUAL; }
  "|"                { return BIT_OR; }
  "^"                { return BIT_XOR; }
  "&"                { return BIT_AND; }
  "<<"               { return BIT_SHIFT_LEFT; }
  ">>"               { return BIT_SHIFT_RIGHT; }
  ";"                { return SEMICOLON; }
  ","                { return COMMA; }
  ":"                { return COLON; }
  "."                { return DOT; }
  "["                { return LEFT_BRACKET; }
  "]"                { return RIGHT_BRACKET; }
  "("                { return LEFT_PAREN; }
  ")"                { return RIGHT_PAREN; }
  "{"                { return LEFT_CURLY; }
  "}"                { return RIGHT_CURLY; }
  "**"               { return DOUBLE_ASTERISK; }
  "~"                { return TYLDA; }

  {INT}              { return INT; }
  {FLOAT}            { return FLOAT; }
  {IDENTIFIER}       { return IDENTIFIER; }
  {STRING}           { return STRING; }
  {BYTES}            { return BYTES; }
  {COMMENT}          { return COMMENT; }
  {WHITE_SPACE}      { return WHITE_SPACE; }

}

[^] { return BAD_CHARACTER; }
