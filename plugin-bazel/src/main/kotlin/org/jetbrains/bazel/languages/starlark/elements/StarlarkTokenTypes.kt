package org.jetbrains.bazel.languages.starlark.elements

object StarlarkTokenTypes {
  // Whitespaces
  @JvmField
  val SPACE = StarlarkTokenType("SPACE")

  @JvmField
  val TAB = StarlarkTokenType("TAB")

  @JvmField
  val LINE_CONTINUATION = StarlarkTokenType("LINE_CONTINUATION")

  @JvmField
  val LINE_BREAK = StarlarkTokenType("LINE_BREAK")

  // Significant whitespaces
  @JvmField
  val INDENT = StarlarkTokenType("INDENT")

  @JvmField
  val DEDENT = StarlarkTokenType("DEDENT")

  @JvmField
  val INCONSISTENT_DEDENT = StarlarkTokenType("UNCONSISTENT_DEDENT")

  @JvmField
  val STATEMENT_BREAK = StarlarkTokenType("STATEMENT_BREAK")

  // Comment
  @JvmField
  val COMMENT = StarlarkTokenType("COMMENT")

  // Literals
  @JvmField
  val STRING = StarlarkTokenType("STRING")

  @JvmField
  val BYTES = StarlarkTokenType("BYTES")

  @JvmField
  val INT = StarlarkTokenType("INT")

  @JvmField
  val FLOAT = StarlarkTokenType("FLOAT")

  // Keywords
  @JvmField
  val AND_KEYWORD = StarlarkTokenType("and")

  @JvmField
  val BREAK_KEYWORD = StarlarkTokenType("break")

  @JvmField
  val CONTINUE_KEYWORD = StarlarkTokenType("continue")

  @JvmField
  val DEF_KEYWORD = StarlarkTokenType("def")

  @JvmField
  val ELIF_KEYWORD = StarlarkTokenType("elif")

  @JvmField
  val ELSE_KEYWORD = StarlarkTokenType("else")

  @JvmField
  val FOR_KEYWORD = StarlarkTokenType("for")

  @JvmField
  val IF_KEYWORD = StarlarkTokenType("if")

  @JvmField
  val IN_KEYWORD = StarlarkTokenType("in")

  @JvmField
  val LAMBDA_KEYWORD = StarlarkTokenType("lambda")

  @JvmField
  val LOAD_KEYWORD = StarlarkTokenType("load")

  @JvmField
  val NOT_KEYWORD = StarlarkTokenType("not")

  @JvmField
  val OR_KEYWORD = StarlarkTokenType("or")

  @JvmField
  val PASS_KEYWORD = StarlarkTokenType("pass")

  @JvmField
  val RETURN_KEYWORD = StarlarkTokenType("return")

  // Possible future keywords
  @JvmField
  val AS_KEYWORD = StarlarkTokenType("as")

  @JvmField
  val ASSERT_KEYWORD = StarlarkTokenType("assert")

  @JvmField
  val ASYNC_KEYWORD = StarlarkTokenType("async")

  @JvmField
  val AWAIT_KEYWORD = StarlarkTokenType("await")

  @JvmField
  val CLASS_KEYWORD = StarlarkTokenType("class")

  @JvmField
  val DEL_KEYWORD = StarlarkTokenType("del")

  @JvmField
  val EXCEPT_KEYWORD = StarlarkTokenType("except")

  @JvmField
  val FINALLY_KEYWORD = StarlarkTokenType("finally")

  @JvmField
  val FROM_KEYWORD = StarlarkTokenType("from")

  @JvmField
  val GLOBAL_KEYWORD = StarlarkTokenType("global")

  @JvmField
  val IMPORT_KEYWORD = StarlarkTokenType("import")

  @JvmField
  val IS_KEYWORD = StarlarkTokenType("is")

  @JvmField
  val NONLOCAL_KEYWORD = StarlarkTokenType("nonlocal")

  @JvmField
  val RAISE_KEYWORD = StarlarkTokenType("raise")

  @JvmField
  val TRY_KEYWORD = StarlarkTokenType("try")

  @JvmField
  val WHILE_KEYWORD = StarlarkTokenType("while")

  @JvmField
  val WITH_KEYWORD = StarlarkTokenType("with")

  @JvmField
  val YIELD_KEYWORD = StarlarkTokenType("yield")

  // Identifier
  @JvmField
  val IDENTIFIER = StarlarkTokenType("IDENTIFIER")

  // Punctuation
  @JvmField
  val PLUS = StarlarkTokenType("+")

  @JvmField
  val MINUS = StarlarkTokenType("-")

  @JvmField
  val MULT = StarlarkTokenType("*")

  @JvmField
  val DIV = StarlarkTokenType("/")

  @JvmField
  val FLOORDIV = StarlarkTokenType("//")

  @JvmField
  val PERC = StarlarkTokenType("%")

  @JvmField
  val EXP = StarlarkTokenType("**")

  @JvmField
  val TILDE = StarlarkTokenType("~")

  @JvmField
  val AND = StarlarkTokenType("&")

  @JvmField
  val OR = StarlarkTokenType("|")

  @JvmField
  val XOR = StarlarkTokenType("^")

  @JvmField
  val LTLT = StarlarkTokenType("<<")

  @JvmField
  val GTGT = StarlarkTokenType(">>")

  @JvmField
  val DOT = StarlarkTokenType(".")

  @JvmField
  val COMMA = StarlarkTokenType(",")

  @JvmField
  val EQ = StarlarkTokenType("=")

  @JvmField
  val SEMICOLON = StarlarkTokenType(";")

  @JvmField
  val COLON = StarlarkTokenType(":")

  @JvmField
  val LPAR = StarlarkTokenType("(")

  @JvmField
  val RPAR = StarlarkTokenType(")")

  @JvmField
  val LBRACKET = StarlarkTokenType("[")

  @JvmField
  val RBRACKET = StarlarkTokenType("]")

  @JvmField
  val LBRACE = StarlarkTokenType("{")

  @JvmField
  val RBRACE = StarlarkTokenType("}")

  @JvmField
  val LT = StarlarkTokenType("<")

  @JvmField
  val GT = StarlarkTokenType(">")

  @JvmField
  val LE = StarlarkTokenType("<=")

  @JvmField
  val GE = StarlarkTokenType(">=")

  @JvmField
  val EQEQ = StarlarkTokenType("==")

  @JvmField
  val NE = StarlarkTokenType("!=")

  @JvmField
  val PLUSEQ = StarlarkTokenType("+=")

  @JvmField
  val MINUSEQ = StarlarkTokenType("-=")

  @JvmField
  val MULTEQ = StarlarkTokenType("*=")

  @JvmField
  val DIVEQ = StarlarkTokenType("/=")

  @JvmField
  val FLOORDIVEQ = StarlarkTokenType("//=")

  @JvmField
  val PERCEQ = StarlarkTokenType("%=")

  @JvmField
  val ANDEQ = StarlarkTokenType("&=")

  @JvmField
  val OREQ = StarlarkTokenType("|=")

  @JvmField
  val XOREQ = StarlarkTokenType("^=")

  @JvmField
  val LTLTEQ = StarlarkTokenType("<<=")

  @JvmField
  val GTGTEQ = StarlarkTokenType(">>=")
}
