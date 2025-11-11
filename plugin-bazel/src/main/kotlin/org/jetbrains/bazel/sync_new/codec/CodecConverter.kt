package org.jetbrains.bazel.sync_new.codec

interface CodecConverter<U, V> {
  fun to(value: U): V
  fun from(value: V): U
}

fun <U, V> converterOf(to: (U) -> V, from: (V) -> U): CodecConverter<U, V> =
  object : CodecConverter<U, V> {
    override fun to(value: U): V = to(value)
    override fun from(value: V): U = from(value)
  }

fun <U, V> CodecConverter<U, V>.flip(): CodecConverter<V, U> = converterOf(to = { from(it) }, from = { to(it) })
