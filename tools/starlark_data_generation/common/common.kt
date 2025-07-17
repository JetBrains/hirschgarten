package common

import com.google.gson.Gson
import com.google.gson.GsonBuilder

enum class Environment {
  ALL,
  BZL,
  BUILD,
  MODULE,
  REPO,
  VENDOR,
}

data class Param(
  val name: String,
  val doc: String?,
  val defaultValue: String,
  val named: Boolean,
  val positional: Boolean,
  val required: Boolean,
)

data class GlobalFunction(
  val name: String,
  val doc: String?,
  val environment: List<Environment>,
  val params: List<Param>,
)

fun serializeFunctionsTo(functions: List<GlobalFunction>): String {
  val gson: Gson = GsonBuilder().setPrettyPrinting().create()
  return gson.toJson(functions)
}
