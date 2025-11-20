package org.jetbrains.bazel.sync_new.connector

import java.nio.file.Path

// TODO: add required values on-demand
sealed interface Value {
  data class VBool(val value: Boolean) : Value
  data class VFile(val path: Path) : Value
  data class VText(val text: String) : Value
  data class VInt(val number: Int) : Value
  data class VFloat(val number: Float) : Value
}

fun argValueOf(obj: Any): Value = when (obj) {
  is Boolean -> Value.VBool(obj)
  is Path -> Value.VFile(obj)
  is String -> Value.VText(obj)
  is Int -> Value.VInt(obj)
  is Float -> Value.VFloat(obj)
  else -> error("unsupported value type: ${obj.javaClass.name}")
}

inline fun <reified T : Value> Value.require(): T = this as? T ?: error("expected ${T::class.java.name}, got ${this.javaClass.name}")
