import com.google.devtools.build.lib.starlarkdocextract.StardocOutputProtos
import common.serializeFunctionsTo
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunction
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctionParameter
import org.jetbrains.bazel.languages.starlark.bazel.Environment
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

  private fun defaultNameOrNull(name: String): String? = if (name == "") null else name

  private fun unwrapName(name: String): String =
    if (name.contains(".")) {
      name.split(".")[1]
    } else {
      name
    }

  private fun attributeInfoToData(attrInfo: StardocOutputProtos.AttributeInfo): BazelGlobalFunctionParameter =
    BazelGlobalFunctionParameter(
      name = attrInfo.name,
      doc = if (attrInfo.docString.isEmpty()) null else replaceTicks(removeLinks(attrInfo.docString)),
      required = attrInfo.mandatory,
      defaultValue = defaultNameOrNull(attrInfo.defaultValue),
      positional = false,
      named = true,
    )

  private fun ruleInfoToData(ruleInfo: StardocOutputProtos.RuleInfo): BazelGlobalFunction {
    val attributes = ruleInfo.attributeList.map { attributeInfoToData(it) }

    return BazelGlobalFunction(
      name = unwrapName(ruleInfo.ruleName),
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

    val allRules = mutableListOf<BazelGlobalFunction>()

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
