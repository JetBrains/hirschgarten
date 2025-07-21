package common

import com.google.gson.Gson
import com.google.gson.GsonBuilder

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

fun serializeFunctionsTo(functions: List<BazelGlobalFunction>): String {
  val gson: Gson = GsonBuilder().setPrettyPrinting().create()
  return gson.toJson(functions)
}
