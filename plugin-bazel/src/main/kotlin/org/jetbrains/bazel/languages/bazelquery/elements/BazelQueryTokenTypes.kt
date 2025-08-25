package org.jetbrains.bazel.languages.bazelquery.elements

object BazelQueryTokenTypes {
  @JvmField
  val COMMENT = BazelQueryTokenType("COMMENT")

  @JvmField
  val DOUBLE_QUOTE = BazelQueryTokenType("\"")

  @JvmField
  val SINGLE_QUOTE = BazelQueryTokenType("'")

  @JvmField
  val LEFT_PAREN = BazelQueryTokenType("(")

  @JvmField
  val RIGHT_PAREN = BazelQueryTokenType(")")

  @JvmField
  val ALLPATHS = BazelQueryTokenType("ALLPATHS")

  @JvmField
  val ATTR = BazelQueryTokenType("ATTR")

  @JvmField
  val BUILDFILES = BazelQueryTokenType("BUILDFILES")

  @JvmField
  val RBUILDFILES = BazelQueryTokenType("RBUILDFILES")

  @JvmField
  val DEPS = BazelQueryTokenType("DEPS")

  @JvmField
  val FILTER = BazelQueryTokenType("FILTER")

  @JvmField
  val KIND = BazelQueryTokenType("KIND")

  @JvmField
  val LABELS = BazelQueryTokenType("LABELS")

  @JvmField
  val LOADFILES = BazelQueryTokenType("LOADFILES")

  @JvmField
  val RDEPS = BazelQueryTokenType("RDEPS")

  @JvmField
  val ALLRDEPS = BazelQueryTokenType("ALLRDEPS")

  @JvmField
  val SAME_PKG_DIRECT_RDEPS = BazelQueryTokenType("SAME_PKG_DIRECT_RDEPS")

  @JvmField
  val SIBLINGS = BazelQueryTokenType("SIBLINGS")

  @JvmField
  val SOME = BazelQueryTokenType("SOME")

  @JvmField
  val SOMEPATH = BazelQueryTokenType("SOMEPATH")

  @JvmField
  val TESTS = BazelQueryTokenType("TESTS")

  @JvmField
  val VISIBLE = BazelQueryTokenType("VISIBLE")

  @JvmField
  val UNQUOTED_WORD = BazelQueryTokenType("UNQUOTED_WORD")

  @JvmField
  val SQ_WORD = BazelQueryTokenType("SQ_WORD")

  @JvmField
  val DQ_WORD = BazelQueryTokenType("DQ_WORD")

  @JvmField
  val ERR_WORD = BazelQueryTokenType("ERR_WORD")

  @JvmField
  val SQ_PATTERN = BazelQueryTokenType("SQ_PATTERN")

  @JvmField
  val DQ_PATTERN = BazelQueryTokenType("DQ_PATTERN")

  @JvmField
  val COLON = BazelQueryTokenType(":")

  @JvmField
  val COMMA = BazelQueryTokenType(",")

  @JvmField
  val FLAG = BazelQueryTokenType("FLAG")

  @JvmField
  val FLAG_NO_VAL = BazelQueryTokenType("FLAG_NO_VAL")

  @JvmField
  val UNFINISHED_FLAG = BazelQueryTokenType("UNFINISHED_FLAG")

  @JvmField
  val UNQUOTED_VAL = BazelQueryTokenType("UNQUOTED_VAL")

  @JvmField
  val SQ_VAL = BazelQueryTokenType("SQ_VAL")

  @JvmField
  val DQ_VAL = BazelQueryTokenType("DQ_VAL")

  @JvmField
  val UNION = BazelQueryTokenType("UNION")

  @JvmField
  val EXCEPT = BazelQueryTokenType("EXCEPT")

  @JvmField
  val INTERSECT = BazelQueryTokenType("INTERSECT")

  @JvmField
  val LET = BazelQueryTokenType("LET")

  @JvmField
  val IN = BazelQueryTokenType("IN")

  @JvmField
  val EQUALS = BazelQueryTokenType("EQUALS")

  @JvmField
  val SET = BazelQueryTokenType("SET")

  @JvmField
  val INTEGER = BazelQueryTokenType("INTEGER")

  @JvmField
  val WHITE_SPACE = BazelQueryTokenType("WHITE_SPACE")

  @JvmField
  val UNEXPECTED = BazelQueryTokenType("UNEXPECTED")

  @JvmField
  val UNFINISHED_VAL = BazelQueryTokenType("UNFINISHED_VAL")

  @JvmField
  val SQ_UNFINISHED = BazelQueryTokenType("SQ_UNFINISHED")

  @JvmField
  val DQ_UNFINISHED = BazelQueryTokenType("DQ_UNFINISHED")

  @JvmField
  val SQ_EMPTY = BazelQueryTokenType("SQ_EMPTY")

  @JvmField
  val DQ_EMPTY = BazelQueryTokenType("DQ_EMPTY")

  @JvmField
  val MISSING_SPACE = BazelQueryTokenType("MISSING_SPACE")
}
