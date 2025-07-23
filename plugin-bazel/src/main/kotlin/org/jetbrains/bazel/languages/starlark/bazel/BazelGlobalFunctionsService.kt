package org.jetbrains.bazel.languages.starlark.bazel

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

/**
 * Global function definitions are stored in JSON files bundled with the plugin.
 *
 * The current implementation loads function definitions from JSON at runtime, which has two potential issues:
 * 1. Performance impact during loading of large JSON files
 * 2. No compile-time validation of function definitions
 *
 * If these become problematic, functions can be stored in Kotlin data classes instead, either:
 * - Manually maintained in code
 * - Auto-generated from JSON during build via a script
 */
@Service(Service.Level.APP)
class BazelGlobalFunctionsService {
  private val globalFunctionsPath = "/bazelGlobalFunctions/global_functions.json"
  private val buildRulesPath = "/bazelGlobalFunctions/rules.json"

  private val globalFunctions: Map<String, BazelGlobalFunction>

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
    globalFunctions = (loadFunctionsList(globalFunctionsPath) + loadFunctionsList(buildRulesPath)).associateBy { it.name }
  }

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

  companion object {
    fun getInstance(): BazelGlobalFunctionsService = service<BazelGlobalFunctionsService>()
  }
}
