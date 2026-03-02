package org.jetbrains.bazel.languages.starlark.bazel

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
enum class Environment {
  BZL,
  BUILD,
  MODULE,
  REPO,
  VENDOR,
}

@ApiStatus.Internal
data class BazelGlobalFunctionParameter(
  val name: String,
  val doc: String?,
  val defaultValue: String?,
  val named: Boolean,
  val positional: Boolean,
  val required: Boolean,
)

internal fun BazelGlobalFunctionParameter.isKwArgs() = name.startsWith("**")

internal fun BazelGlobalFunctionParameter.isVarArgs() = name.startsWith("*")

@ApiStatus.Internal
data class BazelGlobalFunction(
  val name: String,
  val doc: String?,
  val environment: List<Environment>,
  val params: List<BazelGlobalFunctionParameter>,
)

@ApiStatus.Internal
interface StarlarkGlobalFunctionProvider {
  val functions: List<BazelGlobalFunction>

  companion object {
    val extensionPoint = ExtensionPointName<StarlarkGlobalFunctionProvider>("org.jetbrains.bazel.starlarkGlobalFunctionProvider")
  }
}

internal class DefaultBazelGlobalFunctionProvider : StarlarkGlobalFunctionProvider {
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

@ApiStatus.Internal
class BazelGlobalFunctions {
  companion object {
    // These properties read from the extension point dynamically to support test isolation.
    // Extension points can be masked/replaced during tests, so caching the initial value
    // would cause subsequent tests to see stale data.
    internal val globalFunctions: Map<String, BazelGlobalFunction>
      get() = StarlarkGlobalFunctionProvider.extensionPoint.extensionList
        .flatMap { it.functions }
        .associateBy { it.name }

    internal val buildGlobalFunctions: Map<String, BazelGlobalFunction>
      get() = globalFunctions.filter { it.value.environment.contains(Environment.BUILD) }

    val moduleGlobalFunctions: Map<String, BazelGlobalFunction>
      get() = globalFunctions.filter { it.value.environment.contains(Environment.MODULE) }

    internal val extensionGlobalFunctions: Map<String, BazelGlobalFunction>
      get() = globalFunctions.filter { it.value.environment.contains(Environment.BZL) }

    internal val starlarkGlobalFunctions: Map<String, BazelGlobalFunction>
      get() = globalFunctions.filter { it.value.environment.containsAll(Environment.entries) }

    fun getFunctionByName(name: String): BazelGlobalFunction? = globalFunctions[name]
  }
}
