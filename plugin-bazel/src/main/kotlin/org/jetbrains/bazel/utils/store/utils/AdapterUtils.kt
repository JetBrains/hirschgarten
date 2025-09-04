package org.jetbrains.bazel.utils.store.utils

import com.dynatrace.hash4j.hashing.HashValue128
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.sdkcompat.createHashStream128
import org.jetbrains.bazel.target.computeLabelHash
import org.jetbrains.bazel.utils.store.KVStore
import org.jetbrains.bazel.utils.store.Key2HashValue128KVStoreAdapter

val hasher = createHashStream128()

fun <V> KVStore<HashValue128, V>.toLabelStore(): KVStore<Label, V> = Key2HashValue128KVStoreAdapter(
  hasher = { computeLabelHash(it.assumeResolved(), hasher) },
  inner = this,
)
