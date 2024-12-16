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
    val WORD = BazelqueryTokenType("WORD")


    @JvmField
    val COLON = BazelqueryTokenType(":")

    @JvmField
    val COMMA = BazelqueryTokenType(",")

    @JvmField
    val DOUBLE_HYPHEN = BazelqueryTokenType("--")



    @JvmField
    val FLAG = BazelqueryTokenType("FLAG")

    @JvmField
    val VALUE = BazelqueryTokenType("VALUE")




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
    val NEW_LINE = BazelqueryTokenType("NEW_LINE")

}
