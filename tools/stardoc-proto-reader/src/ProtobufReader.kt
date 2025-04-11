import com.google.devtools.build.lib.starlarkdocextract.StardocOutputProtos
import java.io.FileInputStream
import kotlin.system.exitProcess

object ProtobufReader {
  private fun removeLinks(input: String): String {
    var result = input.replace(Regex("<a[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL), "$1")
    result = result.replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
    return result
  }

  private fun replaceTicks(input: String): String {
    var result = input.replace(Regex("```(.*)```", RegexOption.DOT_MATCHES_ALL), "<code>$1</code>")
    result = result.replace(Regex("`(.*?)`", RegexOption.DOT_MATCHES_ALL), "<code>$1</code>")
    return result
  }

  private fun defaultNameOrEmtpyQuotes(name: String): String = if (name == "") "\"\"" else name

  private fun attributeInfoToString(attrInfo: StardocOutputProtos.AttributeInfo): String =
    "BazelNativeRuleArgument(" +
      "\"" + attrInfo.name + "\"" + "," +
      "\"\"\"" + defaultNameOrEmtpyQuotes(attrInfo.defaultValue) + "\"\"\"" + "," +
      attrInfo.mandatory + "," +
      "\"\"\"" + replaceTicks(removeLinks(attrInfo.docString)) + "\"\"\"" +
      ")"

  private fun ruleInfoToString(ruleInfo: StardocOutputProtos.RuleInfo): String {
    var attributes = "setOf("
    for (attr in ruleInfo.attributeList) {
      attributes += attributeInfoToString(attr)
      attributes += ","
    }
    attributes += ")"

    return "BazelNativeRule(" +
      "\"" + ruleInfo.ruleName + "\"" + "," +
      "null" + "," +
      attributes + "," +
      "\"\"\"" + replaceTicks(removeLinks(ruleInfo.docString)) + "\"\"\"" +
      ")"
  }

  @JvmStatic
  fun main(args: Array<String>) {
    if (args.isEmpty()) {
      println("Usage: reader <input_file>")
      exitProcess(1)
    }

    var generatedCode = "package org.jetbrains.bazel.languages.starlark.bazel\n"
    generatedCode += "val RULES = listOf("

    for (inputFilePath in args) {
      val moduleInfo =
        FileInputStream(inputFilePath).use { input ->
          StardocOutputProtos.ModuleInfo.parseFrom(input)
        }

      generatedCode += "setOf("

      for (rule in moduleInfo.ruleInfoList) {
        val ruleInfo = ruleInfoToString(rule)
        generatedCode += ruleInfo
        generatedCode += ","
      }

      generatedCode += "),"
    }

    generatedCode += ")"

    println(generatedCode)
  }
}
