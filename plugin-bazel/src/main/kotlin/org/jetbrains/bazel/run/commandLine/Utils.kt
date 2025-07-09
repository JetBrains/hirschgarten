package org.jetbrains.bazel.run.commandLine

class ProgramArgumentLexer(private val input: String) {
  enum class State {
    Normal, String
  }

  var state: State = State.Normal
  var position: Int = 0
  val args = mutableListOf<String>()

  val eof
    get() = position >= input.length
  val cursor
    get() = if (eof) {
      0
    } else {
      input[position]
    }

  fun parse(): List<String> {
    while (!eof) {
      getToken()
      while (!eof && cursor == ' ') {
        advance()
      }
    }
    return args
  }

  private fun advance() {
    position += 1
  }

  private fun getToken() {
    when (state) {
      State.Normal -> args.add(getNormal() ?: return)
      State.String -> args.add(getString() ?: return)
    }
  }

  private fun getNormal(): String? {
    if (eof) {
      return null
    }

    if (cursor == '"') {
      state = State.String
      advance()
      return null
    }

    val start = position
    while (!eof && cursor != ' ' && cursor != '"') {
      advance()
    }
    return input.substring(start, position)
  }

  private fun getString(): String? {
    if (eof) return null

    val start = position
    while (!eof) {
      if (cursor == '\\') {
        advance()
        if (!eof) advance()
      } else if (cursor == '"') {
        val result = input.substring(start, position)
        advance()
        state = State.Normal
        return result
      } else {
        advance()
      }
    }
    return input.substring(start, position)
  }
}

fun transformProgramArguments(input: String?): List<String> {
  val input = input?.trim() ?: return emptyList()
  return ProgramArgumentLexer(input).parse()
}
