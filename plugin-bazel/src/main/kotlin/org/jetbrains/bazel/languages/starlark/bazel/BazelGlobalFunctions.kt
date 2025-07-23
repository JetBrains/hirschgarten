package org.jetbrains.bazel.languages.starlark.bazel

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.extensions.ExtensionPointName

enum class Environment {
  BZL,
  BUILD,
  MODULE,
  REPO,
  VENDOR,
}

data class BazelGlobalFunctionParameter(
  val name: String,
  val doc: String?,
  val defaultValue: String?,
  val named: Boolean,
  val positional: Boolean,
  val required: Boolean,
)

data class BazelGlobalFunction(
  val name: String,
  val doc: String?,
  val environment: List<Environment>,
  val params: List<BazelGlobalFunctionParameter>,
)

interface StarlarkGlobalFunctionProvider {
  val functions: List<BazelGlobalFunction>

  companion object {
    val extensionPoint = ExtensionPointName<StarlarkGlobalFunctionProvider>("org.jetbrains.bazel.starlarkGlobalFunctionProvider")
  }
}

class DefaultBazelGlobalFunctionProvider : StarlarkGlobalFunctionProvider {
  private val globalFunctionsPath = "/bazelGlobalFunctions/global_functions.json"
  private val buildRulesPath = "/bazelGlobalFunctions/rules.json"

  private fun loadFunctionsList(filePath: String): List<BazelGlobalFunction> {
    val resource = javaClass.getResourceAsStream(filePath)
    val functions = mutableListOf<BazelGlobalFunction>()
    val type = object : TypeToken<List<BazelGlobalFunction>>() {}.type
    (resource?.reader()?.use { Gson().fromJson<List<BazelGlobalFunction>>(it, type) })?.let {
      functions.addAll(it)
    }
    return functions
  }

  private fun loadFunctionsFromJson(): List<BazelGlobalFunction> =
    (loadFunctionsList(globalFunctionsPath) + loadFunctionsList(buildRulesPath))

  override val functions: List<BazelGlobalFunction> = loadFunctionsFromJson()
}

class BazelGlobalFunctions {
  companion object {
    val globalFunctions: Map<String, BazelGlobalFunction> =
      StarlarkGlobalFunctionProvider.extensionPoint.extensionList
        .flatMap {
          it.functions
        }.associateBy { it.name }

    val buildGlobalFunctions: Map<String, BazelGlobalFunction> =
      globalFunctions.filter {
        it.value.environment.contains(Environment.BUILD)
      }

    val moduleGlobalFunctions: Map<String, BazelGlobalFunction> =
      globalFunctions.filter {
        it.value.environment.contains(Environment.MODULE)
      }

    val extensionGlobalFunctions: Map<String, BazelGlobalFunction> =
      globalFunctions.filter {
        it.value.environment.contains(Environment.BZL)
      }

    val starlarkGlobalFunctions: Map<String, BazelGlobalFunction> =
      globalFunctions.filter {
        it.value.environment.containsAll(Environment.entries)
      }

    fun getFunctionByName(name: String): BazelGlobalFunction? = globalFunctions[name]
  }
}
