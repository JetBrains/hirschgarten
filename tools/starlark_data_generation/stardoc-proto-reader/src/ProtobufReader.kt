import com.google.devtools.build.lib.starlarkdocextract.StardocOutputProtos
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import common.Environment
import common.GlobalFunction
import common.Param
import common.serializeFunctionsTo
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

  private fun defaultNameOrEmptyQuotes(name: String): String = if (name == "") "\"\"" else name

  private fun attributeInfoToData(attrInfo: StardocOutputProtos.AttributeInfo): Param =
    Param(
      name = attrInfo.name,
      doc = if (attrInfo.docString.isEmpty()) null else replaceTicks(removeLinks(attrInfo.docString)),
      required = attrInfo.mandatory,
      defaultValue = defaultNameOrEmptyQuotes(attrInfo.defaultValue),
      positional = false,
      named = true,
    )

  private fun ruleInfoToData(ruleInfo: StardocOutputProtos.RuleInfo): GlobalFunction {
    val attributes = ruleInfo.attributeList.map { attributeInfoToData(it) }

    return GlobalFunction(
      name = ruleInfo.ruleName,
      doc = replaceTicks(removeLinks(ruleInfo.docString)),
      environment = listOf(Environment.BUILD),
      params = attributes,
    )
  }

  @JvmStatic
  fun main(args: Array<String>) {
    if (args.isEmpty()) {
      println("Usage: reader <input_file>")
      exitProcess(1)
    }

    val allRules = mutableListOf<GlobalFunction>()

    for (inputFilePath in args) {
      val moduleInfo =
        FileInputStream(inputFilePath).use { input ->
          StardocOutputProtos.ModuleInfo.parseFrom(input)
        }

      for (rule in moduleInfo.ruleInfoList) {
        val ruleData = ruleInfoToData(rule)
        allRules.add(ruleData)
      }
    }

    println(serializeFunctionsTo(allRules))
  }
}
