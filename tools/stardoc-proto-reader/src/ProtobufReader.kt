import com.google.devtools.build.lib.starlarkdocextract.StardocOutputProtos
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.FileInputStream
import kotlin.system.exitProcess

object ProtobufReader {
  private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

  data class AttributeInfo(
    val name: String,
    val docString: String?,
    val required: Boolean,
    val default: String,
    val positional: Boolean = false,
  )

  data class RuleInfo(
    val name: String,
    val docString: String,
    val params: List<AttributeInfo>,
  )

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

  private fun attributeInfoToData(attrInfo: StardocOutputProtos.AttributeInfo): AttributeInfo =
    AttributeInfo(
      name = attrInfo.name,
      docString = if (attrInfo.docString.isEmpty()) null else replaceTicks(removeLinks(attrInfo.docString)),
      required = attrInfo.mandatory,
      default = defaultNameOrEmptyQuotes(attrInfo.defaultValue),
      positional = false,
    )

  private fun ruleInfoToData(ruleInfo: StardocOutputProtos.RuleInfo): RuleInfo {
    val attributes = ruleInfo.attributeList.map { attributeInfoToData(it) }

    return RuleInfo(
      name = ruleInfo.ruleName,
      docString = replaceTicks(removeLinks(ruleInfo.docString)),
      params = attributes,
    )
  }

  @JvmStatic
  fun main(args: Array<String>) {
    if (args.isEmpty()) {
      println("Usage: reader <input_file>")
      exitProcess(1)
    }

    val allRules = mutableListOf<RuleInfo>()

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

    val jsonOutput = gson.toJson(allRules)
    println(jsonOutput)
  }
}
