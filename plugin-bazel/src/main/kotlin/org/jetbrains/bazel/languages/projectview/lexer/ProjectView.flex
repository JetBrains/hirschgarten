package org.jetbrains.bazel.languages.projectview.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;

%%

%class _ProjectViewLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType

INT = [1-9][0-9]* | 0
BOOL = "true" | "false"

COMMENT="#"[^\r\n]*

IDENTIFIER = [^\d\W]\w*

STRING = \"{STRING_ITEM}*(\")?
STRING_ITEM = {CHAR} | {ESCAPE_SEQUENCE}
CHAR = [^\\\n\"]

ESCAPE_SEQUENCE = \\[^]
%%

[\ ]                                     { return ProjectViewTokenTypes.SPACE; }
[\t]                                     { return ProjectViewTokenTypes.TAB; }
[\n]                                     { return ProjectViewTokenTypes.LINE_BREAK; }

{COMMENT}                                { return ProjectViewTokenTypes.COMMENT; }
{STRING}                                 { return ProjectViewTokenTypes.STRING; }
{INT}                                    { return ProjectViewTokenTypes.INT; }
{BOOL}                                   { return ProjectViewTokenTypes.BOOL; }

"import"                                 { return ProjectViewTokenTypes.IMPORT_KEYWORD; }
"targets"                                { return ProjectViewTokenTypes.TARGETS_KEYWORD; }
"bazel_binary"                           { return ProjectViewTokenTypes.BAZEL_BINARY_KEYWORD; }
"directories"                            { return ProjectViewTokenTypes.DIRECTORIES_KEYWORD; }
"derive_targets_from_directories"        { return ProjectViewTokenTypes.DERIVE_TARGETS_FROM_DIRECTORIES_KEYWORD; }
"import_depth"                           { return ProjectViewTokenTypes.IMPORT_DEPTH_KEYWORD; }
"workspace_type"                         { return ProjectViewTokenTypes.WORKSPACE_TYPE_KEYWORD; }
"additional_languages"                   { return ProjectViewTokenTypes.ADDITIONAL_LANGUAGES_KEYWORD; }
"java_language_level"                    { return ProjectViewTokenTypes.JAVA_LANGUAGE_LEVEL_KEYWORD; }
"test_sources"                           { return ProjectViewTokenTypes.TEST_SOURCES_KEYWORD; }
"shard_sync"                             { return ProjectViewTokenTypes.SHARD_SYNC_KEYWORD; }
"target_shard_size"                      { return ProjectViewTokenTypes.TARGET_SHARD_SIZE_KEYWORD; }
"exclude_library"                        { return ProjectViewTokenTypes.EXCLUDE_LIBRARY_KEYWORD; }
"build_flags"                            { return ProjectViewTokenTypes.BUILD_FLAGS_KEYWORD; }
"sync_flags"                             { return ProjectViewTokenTypes.SYNC_FLAGS_KEYWORD; }
"test_flags"                             { return ProjectViewTokenTypes.TEST_FLAGS_KEYWORD; }
"import_run_configuration"               { return ProjectViewTokenTypes.IMPORT_RUN_CONFIGURATION_KEYWORD; }
"android_sdk_platform"                   { return ProjectViewTokenTypes.ANDROID_SDK_PLATFORM_KEYWORD; }
"android_min_sdk"                        { return ProjectViewTokenTypes.ANDROID_MIN_SDK_KEYWORD; }
"generated_android_resource_directories" { return ProjectViewTokenTypes.GENERATED_ANDROID_RESOURCE_DIRECTORIES_KEYWORD; }
"ts_config_rules"                        { return ProjectViewTokenTypes.TS_CONFIG_RULES_KEYWORD; }
"allow_manual_targets_sync"              { return ProjectViewTokenTypes.ALLOW_MANUAL_TARGETS_SYNC; }
"enabled_rules"                          { return ProjectViewTokenTypes.ENABLED_RULES_KEYWORD; }
"produce_trace_log"                      { return ProjectViewTokenTypes.PRODUCE_TRACE_LOG_KEYWORD; }

{IDENTIFIER}                             { return ProjectViewTokenTypes.IDENTIFIER; }

"-"                                      { return ProjectViewTokenTypes.MINUS; }
"*"                                      { return ProjectViewTokenTypes.MULT; }
"/"                                      { return ProjectViewTokenTypes.DIV; }
"."                                      { return ProjectViewTokenTypes.DOT; }
"="                                      { return ProjectViewTokenTypes.EQ; }
":"                                      { return ProjectViewTokenTypes.COLON; }

[^]                                      { return TokenType.BAD_CHARACTER; }
