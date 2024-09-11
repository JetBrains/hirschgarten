package org.jetbrains.bazel.languages.bazelrc.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes;

@SuppressWarnings("UnnecessaryUnicodeEscape")
%%
%class _BazelrcLexer
%implements FlexLexer
%no_suppress_warnings
%unicode
%function advance
%type IElementType

NL=[\r\n]
SPACE=[\ \t]
COMMENT=#[^\r\n]*

SOFT_NL=\\\r?\n
SQ=[']
DQ=[\"]

COLON=[:]

COMMAND={SOFT_NL} | [^{COLON}{SQ}{DQ}{SPACE}{NL}]

// lexing states:
%xstate CMD, CONFIG
%xstate CMD_DQ, CONFIG_DQ
%xstate CMD_SQ, CONFIG_SQ

%xstate FLAGS

%%

//
//"adsfa adsf adsf asd: adsfasdfasd asdfasdf
//
//asdfas:

// [(Bazelrc:", 0, 1), (Bazelrc:COMMAND, 1, 20), (Bazelrc::, 20, 21), (Bazelrc:CONFIG, 21, 42), (BAD_CHARACTER, 42, 53)]

<YYINITIAL> {
    [{SPACE}{NL}]+                              { return TokenType.WHITE_SPACE; }
    {COMMENT}+                                  { return BazelrcTokenTypes.COMMENT; }

    [^]                                         { yybegin(CMD); yypushback(1); }
}

<CMD> {
    {SQ}                                        { yybegin(CMD_SQ); return BazelrcTokenTypes.SINGLE_QUOTE; }
    {DQ}                                        { yybegin(CMD_DQ); return BazelrcTokenTypes.DOUBLE_QUOTE; }

    {COMMAND}+                                  { return BazelrcTokenTypes.COMMAND; }

    {COLON}                                     { yybegin(CONFIG); return BazelrcTokenTypes.COLON; }

    {SPACE}+                                    { yybegin(FLAGS); return TokenType.WHITE_SPACE; }

    [^]                                         { yybegin(FLAGS); yypushback(1); }
}

<CONFIG>  {
    ({SOFT_NL} | [^{SPACE}{NL}] )+              { yybegin(FLAGS); return BazelrcTokenTypes.CONFIG; }
}

<CMD_DQ> {
    (\\{DQ} | [{SPACE}{SQ}] | {COMMAND})+      { return BazelrcTokenTypes.COMMAND; }
    {COLON}                                    { yybegin(CONFIG_DQ); return BazelrcTokenTypes.COLON;}

    {DQ}                                       { yybegin(FLAGS); return BazelrcTokenTypes.DOUBLE_QUOTE; }
}

<CMD_SQ> {
    (\\{SQ} | [{SPACE}{DQ}] | {COMMAND})+      { return BazelrcTokenTypes.COMMAND; }
    {COLON}                                    { yybegin(CONFIG_SQ); return BazelrcTokenTypes.COLON;}

    {SQ}                                       { yybegin(FLAGS); return BazelrcTokenTypes.SINGLE_QUOTE; }
}

<CONFIG_DQ> {
    (\\{DQ} | [{SPACE}{SQ}] | {COMMAND})+      { return BazelrcTokenTypes.CONFIG; }

    {DQ}                                       { yybegin(FLAGS); return BazelrcTokenTypes.DOUBLE_QUOTE; }
}

<CONFIG_SQ> {
    (\\{SQ} | [{SPACE}{DQ}] | {COMMAND})+       { return BazelrcTokenTypes.CONFIG; }

    {SQ}                                        { yybegin(FLAGS); return BazelrcTokenTypes.SINGLE_QUOTE; }
}

<CMD, CONFIG, CMD_SQ, CONFIG_SQ, CMD_DQ, CONFIG_DQ> {
    {NL}+[{SPACE}{NL}]*                         { yybegin(YYINITIAL); return TokenType.WHITE_SPACE; }
    [^]                                         { yybegin(FLAGS); yypushback(1); }
}

<FLAGS> {
    ({SOFT_NL} | [^{SPACE}{NL}])+               { return BazelrcTokenTypes.FLAG; }

    {DQ} ( {SOFT_NL} | \\{DQ} | [^{NL}{DQ}] )*  { return BazelrcTokenTypes.FLAG; }
    {SQ} ( {SOFT_NL} | \\{SQ} | [^{NL}{SQ}] )*  { return BazelrcTokenTypes.FLAG; }

    {SPACE}+                                    { return TokenType.WHITE_SPACE; }

    {COMMENT}+                                  { yybegin(YYINITIAL); return BazelrcTokenTypes.COMMENT; }
    {SPACE}* {NL}+ [{SPACE} {NL}]*              { yybegin(YYINITIAL); return TokenType.WHITE_SPACE; }

//    [^]                                         { return TokenType.BAD_CHARACTER; }
}

// [^]                                         { return TokenType.BAD_CHARACTER; }