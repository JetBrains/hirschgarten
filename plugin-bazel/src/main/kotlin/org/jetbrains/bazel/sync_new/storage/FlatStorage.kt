package org.jetbrains.bazel.sync_new.storage

import org.jetbrains.bazel.sync_new.codec.Codec
import org.jetbrains.bazel.sync_new.codec.CodecBuilder
import kotlin.reflect.KProperty

interface FlatStorage<V> {
  fun get(): V
  fun set(value: V)
  fun modify(op: (value: V) -> V): V
  fun reset()
  fun mark()
}

interface FlatStoreBuilder<V> {
  fun withCreator(func: () -> V): FlatStoreBuilder<V>
  fun withCodec(codec: CodecBuilder.() -> Codec<V>): FlatStoreBuilder<V>
  fun build(): FlatStorage<V>
}

operator fun <T> FlatStorage<T>.getValue(thisRef: Any?, property: KProperty<*>): T = get()
operator fun <T> FlatStorage<T>.setValue(thisRef: Any?, property: KProperty<*>, value: T): Unit = set(value)

fun <T> FlatStorage<T>.mutate(op: (value: T) -> Unit): T = modify {
  op(it)
  it
}

