package org.jetbrains.bazel.languages.starlark.bazel

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
class BazelGlobalFunctionsService {
  private val moduleFunctionsFilePath = "/bazelGlobalFunctions/moduleFunctions.json"
  private val hardcodedBuildRulesFilePath = "/bazelGlobalFunctions/hardcodedBuildRules.json"
  private val generatedBuildRulesFilePath = "/bazelGlobalFunctions/generatedBuildRules.json"

  private val moduleFunctionsMap: Map<String, BazelGlobalFunction>
  private val buildRulesMap: Map<String, BazelGlobalFunction>

  // Apply to all *_binary rules.
  private val binaryRulesCommonArguments =
    setOf(
      BazelGlobalFunctionParameter(
        "args",
        "[]",
        positional = false,
        required = false,
      ),
      BazelGlobalFunctionParameter(
        "env",
        "{}",
        positional = false,
        required = false,
      ),
      BazelGlobalFunctionParameter(
        "output_licenses",
        "[]",
        positional = false,
        required = false,
      ),
    )

  // Apply to all *_test rules.
  private val testRulesCommonArguments =
    setOf(
      BazelGlobalFunctionParameter(
        "args",
        "[]",
        positional = false,
        required = false,
      ),
      BazelGlobalFunctionParameter(
        "env",
        "{}",
        positional = false,
        required = false,
      ),
      BazelGlobalFunctionParameter(
        "env_inherit",
        "[]",
        positional = false,
        required = false,
      ),
      BazelGlobalFunctionParameter(
        "size",
        "\"\"",
        positional = false,
        required = false,
      ),
      BazelGlobalFunctionParameter(
        "timeout",
        "\"\"",
        positional = false,
        required = false,
      ),
      BazelGlobalFunctionParameter(
        "flaky",
        "False",
        positional = false,
        required = false,
      ),
      BazelGlobalFunctionParameter(
        "shard_count",
        "-1",
        positional = false,
        required = false,
      ),
      BazelGlobalFunctionParameter(
        "local",
        "False",
        positional = false,
        required = false,
      ),
    )

  private fun loadFunctionsList(filePath: String): List<BazelGlobalFunction> {
    val resource = javaClass.getResourceAsStream(filePath)
    val functions = mutableListOf<BazelGlobalFunction>()
    val type = object : TypeToken<List<BazelGlobalFunction>>() {}.type
    (resource?.reader()?.use { Gson().fromJson<List<BazelGlobalFunction>>(it, type) })?.let {
      functions.addAll(it)
    }
    return functions
  }

  init {
    moduleFunctionsMap = loadFunctionsList(moduleFunctionsFilePath).associateBy { it.name }
    val buildRules =
      (loadFunctionsList(hardcodedBuildRulesFilePath) + loadFunctionsList(generatedBuildRulesFilePath)).map { func ->
        if (func.name.endsWith("_binary")) {
          func.copy(params = (func.params + binaryRulesCommonArguments).toList())
        } else if (func.name.endsWith("_test")) {
          func.copy(params = (func.params + testRulesCommonArguments).toList())
        } else {
          func
        }
      }
    buildRulesMap = buildRules.associateBy { it.name }
  }

  fun getModuleFunctions(): Map<String, BazelGlobalFunction> = moduleFunctionsMap

  fun getBuildFunctions(): Map<String, BazelGlobalFunction> = buildRulesMap

  fun getFunctionByName(name: String): BazelGlobalFunction? = moduleFunctionsMap[name] ?: buildRulesMap[name]

  companion object {
    fun getInstance(): BazelGlobalFunctionsService = service<BazelGlobalFunctionsService>()
  }
}
