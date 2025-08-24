package common

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunction

fun serializeFunctionsTo(functions: List<BazelGlobalFunction>): String {
  val gson: Gson = GsonBuilder().setPrettyPrinting().create()
  return gson.toJson(functions)
}
