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
/*
    @JvmField
    val VALUE = BazelqueryTokenType("VALUE")*/

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
    val INTEGER = BazelqueryTokenType("INTEGER")

    @JvmField
    val WHITE_SPACE = BazelqueryTokenType("WHITE_SPACE")

    @JvmField
    val SPACE = BazelqueryTokenType("SPACE")


    @JvmField
    val UNEXPECTED = BazelqueryTokenType("UNEXPECTED")
}
