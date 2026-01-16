package org.jetbrains.bazel.bazelrunner.outputs

class OutputCollector : OutputHandler {
  private val lines = mutableListOf<String>()
  private val stringBuilder = StringBuilder()

  override fun onNextLine(line: String) {
    lines.add(line.removeSuffix("\n").removeSuffix("\r"))
    stringBuilder.append(line)
  }

  fun lines(): List<String> = lines.toList()

  fun output(): String = stringBuilder.toString()
}
