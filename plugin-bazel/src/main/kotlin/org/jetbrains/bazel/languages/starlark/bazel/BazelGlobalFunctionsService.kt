package org.jetbrains.bazel.languages.starlark.bazel

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger

@Service(Service.Level.APP)
class BazelGlobalFunctionsService {
  private val logger = logger<BazelGlobalFunctionsService>()
  private val moduleFunctionsFilePath = "/bazelGlobalFunctions/moduleFunctions.json"
  private lateinit var moduleFunctionsMap: Map<String, BazelGlobalFunction>

  init {
    val resource = javaClass.getResourceAsStream(moduleFunctionsFilePath)
    try {
      val moduleFunctions = mutableListOf<BazelGlobalFunction>()
      val type = object : TypeToken<List<BazelGlobalFunction>>() {}.type
      (resource?.reader()?.use { Gson().fromJson<List<BazelGlobalFunction>>(it, type) })?.let {
        moduleFunctions.addAll(it)
      }
      moduleFunctionsMap = moduleFunctions.associateBy { it.name }
    } catch (e: Exception) {
      logger.error("Failed to load bazel global functions", e)
    }
  }

  fun getModuleFunctions(): Map<String, BazelGlobalFunction> = moduleFunctionsMap
}
