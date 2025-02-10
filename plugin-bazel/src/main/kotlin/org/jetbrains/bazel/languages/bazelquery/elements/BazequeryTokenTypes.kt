package org.jetbrains.bazel.languages.bazelquery.elements

object BazelqueryTokenTypes {

    @JvmField
    val COMMENT = BazelqueryTokenType("COMMENT")

    @JvmField
    val DOUBLE_QUOTE = BazelqueryTokenType("\"")

    @JvmField
    val SINGLE_QUOTE = BazelqueryTokenType("'")

    @JvmField
    val LEFT_PAREN = BazelqueryTokenType("(")

    @JvmField
    val RIGHT_PAREN = BazelqueryTokenType(")")


    @JvmField
    val COMMAND = BazelqueryTokenType("COMMAND")


    @JvmField
    val ALLPATHS = BazelqueryTokenType("ALLPATHS")

    @JvmField
    val ATTR = BazelqueryTokenType("ATTR")

    @JvmField
    val BUILDFILES = BazelqueryTokenType("BUILDFILES")

    @JvmField
    val RBUILDFILES = BazelqueryTokenType("RBUILDFILES")

    @JvmField
    val DEPS = BazelqueryTokenType("DEPS")

    @JvmField
    val FILTER = BazelqueryTokenType("FILTER")

    @JvmField
    val KIND = BazelqueryTokenType("KIND")

    @JvmField
    val LABELS = BazelqueryTokenType("LABELS")

    @JvmField
    val LOADFILES = BazelqueryTokenType("LOADFILES")

    @JvmField
    val RDEPS = BazelqueryTokenType("RDEPS")

    @JvmField
    val ALLRDEPS = BazelqueryTokenType("ALLRDEPS")

    @JvmField
    val SAME_PKG_DIRECT_RDEPS = BazelqueryTokenType("SAME_PKG_DIRECT_RDEPS")

    @JvmField
    val SIBLINGS = BazelqueryTokenType("SIBLINGS")

    @JvmField
    val SOME = BazelqueryTokenType("SOME")

    @JvmField
    val SOMEPATH = BazelqueryTokenType("SOMEPATH")

    @JvmField
    val TESTS = BazelqueryTokenType("TESTS")

    @JvmField
    val VISIBLE = BazelqueryTokenType("VISIBLE")





    @JvmField
    val UNQUOTED_WORD = BazelqueryTokenType("UNQUOTED_WORD")

    @JvmField
    val SQ_WORD = BazelqueryTokenType("SQ_WORD")

    @JvmField
      val DQ_WORD = BazelqueryTokenType("DQ_WORD")


    @JvmField
    val COLON = BazelqueryTokenType(":")

    @JvmField
    val COMMA = BazelqueryTokenType(",")

    @JvmField
    val DOUBLE_HYPHEN = BazelqueryTokenType("--")



    @JvmField
    val FLAG = BazelqueryTokenType("FLAG")
    @JvmField
    val FLAG_NO_VAL = BazelqueryTokenType("FLAG_NO_VAL")
    @JvmField
    val UNFINISHED_FLAG = BazelqueryTokenType("UNFINISHED_FLAG")
/*
    @JvmField
    val VALUE = BazelqueryTokenType("VALUE")
*/

    @JvmField
    val UNQUOTED_VAL = BazelqueryTokenType("UNQUOTED_VAL")

    @JvmField
    val SQ_VAL = BazelqueryTokenType("SQ_VAL")

    @JvmField
    val DQ_VAL = BazelqueryTokenType("DQ_VAL")





    @JvmField
    val UNION = BazelqueryTokenType("UNION")

    @JvmField
    val EXCEPT = BazelqueryTokenType("EXCEPT")

    @JvmField
    val INTERSECT = BazelqueryTokenType("INTERSECT")

    @JvmField
    val LET = BazelqueryTokenType("LET")

    @JvmField
    val IN = BazelqueryTokenType("IN")

    @JvmField
    val EQUALS = BazelqueryTokenType("EQUALS")

    @JvmField
    val SET = BazelqueryTokenType("SET")


    @JvmField
    val BAZEL = BazelqueryTokenType("BAZEL")

    @JvmField
    val QUERY = BazelqueryTokenType("QUERY")

  @JvmField
  val BAZEL_NO_SPACE = BazelqueryTokenType("BAZEL_NO_SPACE")

  @JvmField
  val QUERY_NO_SPACE = BazelqueryTokenType("QUERY_NO_SPACE")



    @JvmField
    val INTEGER = BazelqueryTokenType("INTEGER")

    @JvmField
    val WHITE_SPACE = BazelqueryTokenType("WHITE_SPACE")

    @JvmField
    val SPACE = BazelqueryTokenType("SPACE")


    @JvmField
    val UNEXPECTED = BazelqueryTokenType("UNEXPECTED")



    @JvmField
    val UNFINISHED_VAL = BazelqueryTokenType("UNFINISHED_VAL")

    @JvmField
    val SQ_UNFINISHED = BazelqueryTokenType("SQ_UNFINISHED")

    @JvmField
    val DQ_UNFINISHED = BazelqueryTokenType("DQ_UNFINISHED")

    @JvmField
    val SQ_EMPTY = BazelqueryTokenType("SQ_EMPTY")

    @JvmField
    val DQ_EMPTY = BazelqueryTokenType("DQ_EMPTY")

    @JvmField
    val MISSING_SPACE = BazelqueryTokenType("MISSING_SPACE")
}
